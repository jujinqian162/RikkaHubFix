const INLINE_LATEX_REGEX = /\\\((.+?)\\\)/g;
const BLOCK_LATEX_REGEX = /\\\[(.+?)\\\]/gs;
const CODE_BLOCK_REGEX = /```[\s\S]*?```|`[^`\n]*`/g;
const DISPLAY_BRACKET_OPEN_REGEX = /\\\[/g;
const DISPLAY_BRACKET_CLOSE_REGEX = /\\]/g;

type Range = {
  start: number;
  end: number;
};

function findCodeBlocks(content: string): Range[] {
  const codeBlocks: Range[] = [];
  let match;
  const codeBlockRegex = new RegExp(CODE_BLOCK_REGEX.source, "g");
  while ((match = codeBlockRegex.exec(content)) !== null) {
    codeBlocks.push({ start: match.index, end: match.index + match[0].length });
  }
  return codeBlocks;
}

function isInRanges(position: number, ranges: Range[]): boolean {
  return ranges.some((range) => position >= range.start && position < range.end);
}

function countRegexOutsideRanges(content: string, regex: RegExp, ranges: Range[]): number {
  let count = 0;
  const matcher = new RegExp(regex.source, "g");
  let match;
  while ((match = matcher.exec(content)) !== null) {
    if (!isInRanges(match.index, ranges)) {
      count += 1;
    }
  }
  return count;
}

function closeTrailingUnmatchedDisplayBracketMath(content: string): string {
  const codeBlocks = findCodeBlocks(content);
  const openCount = countRegexOutsideRanges(content, DISPLAY_BRACKET_OPEN_REGEX, codeBlocks);
  const closeCount = countRegexOutsideRanges(content, DISPLAY_BRACKET_CLOSE_REGEX, codeBlocks);
  if (openCount <= closeCount) return content;

  return `${content.trimEnd()}\n\\]`;
}

function closeTrailingUnmatchedDollarMathBlock(content: string): string {
  const codeBlocks = findCodeBlocks(content);
  const fenceLineStarts: number[] = [];
  let lineStart = 0;

  while (lineStart <= content.length) {
    const lineEnd = content.indexOf("\n", lineStart);
    const lineEndExclusive = lineEnd === -1 ? content.length : lineEnd;
    const lineText = content.slice(lineStart, lineEndExclusive);

    if (lineText.trim() === "$$" && !isInRanges(lineStart, codeBlocks)) {
      fenceLineStarts.push(lineStart);
    }

    if (lineEnd === -1) break;
    lineStart = lineEnd + 1;
  }

  if (fenceLineStarts.length % 2 === 0) return content;
  return `${content.trimEnd()}\n$$`;
}

export function preProcessMarkdown(content: string): string {
  const bracketBalanced = closeTrailingUnmatchedDisplayBracketMath(content);
  const codeBlocks = findCodeBlocks(bracketBalanced);

  let result = bracketBalanced.replace(
    new RegExp(INLINE_LATEX_REGEX.source, "g"),
    (match, group1, offset) => {
      if (isInRanges(offset, codeBlocks)) {
        return match;
      }
      return `$${group1}$`;
    },
  );

  result = result.replace(new RegExp(BLOCK_LATEX_REGEX.source, "gs"), (match, group1, offset) => {
    if (isInRanges(offset, codeBlocks)) {
      return match;
    }
    return `$$${group1}$$`;
  });

  return closeTrailingUnmatchedDollarMathBlock(result);
}

import { describe, expect, test } from "bun:test";
import { unified } from "unified";
import remarkGfm from "remark-gfm";
import remarkMath from "remark-math";
import remarkParse from "remark-parse";
import { preProcessMarkdown } from "./preprocess";

type MarkdownNode = {
  type?: string;
  children?: MarkdownNode[];
};

function containsNodeType(node: MarkdownNode, type: string): boolean {
  if (node.type === type) return true;
  return node.children?.some((child) => containsNodeType(child, type)) ?? false;
}

describe("preProcessMarkdown", () => {
  test("keeps bare plus and minus lines inside an unclosed bracket math block while streaming", () => {
    const content = String.raw`\[
A = \begin{bmatrix}
1 & 2 \\
3 & 4
\end{bmatrix}

+

B = \begin{bmatrix}
5 & 6 \\
7 & 8
\end{bmatrix}

-

C = \begin{bmatrix}
1 & 1 \\
1 & 1
\end{bmatrix}`;

    const processed = preProcessMarkdown(content);
    const tree = unified()
      .use(remarkParse)
      .use(remarkGfm)
      .use(remarkMath)
      .parse(processed) as MarkdownNode;

    expect(containsNodeType(tree, "math")).toBe(true);
    expect(containsNodeType(tree, "list")).toBe(false);
  });

  test("leaves closed bracket math unchanged after conversion", () => {
    const content = String.raw`\[
x + y
\]`;

    expect(preProcessMarkdown(content)).toBe("$$\nx + y\n$$");
  });
});

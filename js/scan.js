const fs = require("fs/promises");
const path = require("path");

/**
 * 扫描目录：列出所有文件，并输出每个文件内容（utf-8）。
 * - 目录为空：提示并退出
 * - 遇到子目录：默认跳过（可改成递归）
 * - 二进制文件：可能乱码，仍按 utf-8 打印（作业通常是文本文件）
 */
async function scanDir(dir) {
  let entries;
  try {
    entries = await fs.readdir(dir, { withFileTypes: true });
  } catch (e) {
    console.error("读取目录失败：", e.message);
    process.exit(1);
  }

  if (entries.length === 0) {
    console.log(`[空目录] ${dir} 目录下没有任何文件。`);
    return;
  }

  const files = entries.filter(e => e.isFile());
  const dirs = entries.filter(e => e.isDirectory());

  console.log(`目录：${dir}`);
  console.log(`文件数：${files.length}，子目录数：${dirs.length}`);
  if (dirs.length) {
    console.log("提示：存在子目录（默认不递归），分别是：", dirs.map(d => d.name).join(", "));
  }
  console.log("=".repeat(60));

  for (const f of files) {
    const filePath = path.join(dir, f.name);
    console.log(`FILE: ${f.name}`);
    console.log("-".repeat(60));
    try {
      const content = await fs.readFile(filePath, "utf-8");
      console.log(content.length ? content : "[空文件]");
    } catch (e) {
      console.log(`[读取失败] ${e.message}`);
    }
    console.log("=".repeat(60));
  }
}

const target = process.argv[2] || ".";
scanDir(path.resolve(target));

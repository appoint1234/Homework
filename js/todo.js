const KEY = "todos_v1";

const input = document.getElementById("todoInput");
const addBtn = document.getElementById("addBtn");
const list = document.getElementById("list");
const stat = document.getElementById("stat");
const clearDoneBtn = document.getElementById("clearDone");

let todos = load();

function load() {
  try {
    return JSON.parse(localStorage.getItem(KEY) || "[]");
  } catch {
    return [];
  }
}
function save() {
  localStorage.setItem(KEY, JSON.stringify(todos));
}
function render() {
  list.innerHTML = "";
  const doneCount = todos.filter(t => t.done).length;
  stat.textContent = `共 ${todos.length} 项，已完成 ${doneCount} 项`;

  todos.forEach((t, idx) => {
    const li = document.createElement("li");
    li.className = "item";

    const left = document.createElement("div");
    left.className = "left";

    const cb = document.createElement("input");
    cb.type = "checkbox";
    cb.checked = t.done;
    cb.addEventListener("change", () => {
      todos[idx].done = cb.checked;
      save();
      render();
    });

    const span = document.createElement("span");
    span.className = "text" + (t.done ? " done" : "");
    span.textContent = t.text;

    left.appendChild(cb);
    left.appendChild(span);

    const del = document.createElement("button");
    del.className = "del";
    del.textContent = "删除";
    del.addEventListener("click", () => {
      todos.splice(idx, 1);
      save();
      render();
    });

    li.appendChild(left);
    li.appendChild(del);
    list.appendChild(li);
  });
}

function addTodo() {
  const text = input.value.trim();
  if (!text) return;
  todos.unshift({ id: Date.now(), text, done: false });
  input.value = "";
  save();
  render();
}

addBtn.addEventListener("click", addTodo);
input.addEventListener("keydown", (e) => {
  if (e.key === "Enter") addTodo();
});

clearDoneBtn.addEventListener("click", () => {
  todos = todos.filter(t => !t.done);
  save();
  render();
});

render();

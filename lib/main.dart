import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() => runApp(const TodoApp());

class TodoApp extends StatelessWidget {
  const TodoApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'TodoList',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(useMaterial3: true, colorSchemeSeed: Colors.blue),
      home: const TodoPage(),
    );
  }
}

class TodoItem {
  final String id;
  final String text;
  final bool done;

  const TodoItem({required this.id, required this.text, required this.done});

  TodoItem copyWith({String? text, bool? done}) =>
      TodoItem(id: id, text: text ?? this.text, done: done ?? this.done);

  Map<String, dynamic> toJson() => {"id": id, "text": text, "done": done};

  static TodoItem fromJson(Map<String, dynamic> j) =>
      TodoItem(id: j["id"], text: j["text"], done: j["done"]);
}

class TodoPage extends StatefulWidget {
  const TodoPage({super.key});

  @override
  State<TodoPage> createState() => _TodoPageState();
}

class _TodoPageState extends State<TodoPage> {
  static const _key = "todos_v1";
  final _controller = TextEditingController();
  final List<TodoItem> _todos = [];
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    final sp = await SharedPreferences.getInstance();
    final raw = sp.getString(_key);
    if (raw != null && raw.isNotEmpty) {
      final list = (jsonDecode(raw) as List)
          .map((e) => TodoItem.fromJson(e as Map<String, dynamic>))
          .toList();
      _todos
        ..clear()
        ..addAll(list);
    }
    setState(() => _loading = false);
  }

  Future<void> _save() async {
    final sp = await SharedPreferences.getInstance();
    final raw = jsonEncode(_todos.map((e) => e.toJson()).toList());
    await sp.setString(_key, raw);
  }

  void _add() {
    final t = _controller.text.trim();
    if (t.isEmpty) return;
    final item = TodoItem(
      id: DateTime.now().microsecondsSinceEpoch.toString(),
      text: t,
      done: false,
    );
    setState(() {
      _todos.insert(0, item); // 新的放最上面
      _controller.clear();
    });
    _save();
  }

  void _toggle(int index) {
    setState(() {
      final old = _todos[index];
      _todos[index] = old.copyWith(done: !old.done);
    });
    _save();
  }

  void _delete(int index) {
    setState(() => _todos.removeAt(index));
    _save();
  }

  void _clearDone() {
    setState(() => _todos.removeWhere((e) => e.done));
    _save();
  }

  @override
  Widget build(BuildContext context) {
    final doneCount = _todos.where((e) => e.done).length;

    return Scaffold(
      appBar: AppBar(
        title: const Text("TodoList"),
        actions: [
          if (doneCount > 0)
            TextButton(
              onPressed: _clearDone,
              child: const Text("清除已完成"),
            ),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : Column(
              children: [
                Padding(
                  padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
                  child: Row(
                    children: [
                      Expanded(
                        child: TextField(
                          controller: _controller,
                          onSubmitted: (_) => _add(),
                          decoration: const InputDecoration(
                            hintText: "输入一个待办…",
                            border: OutlineInputBorder(),
                          ),
                        ),
                      ),
                      const SizedBox(width: 10),
                      FilledButton(
                        onPressed: _add,
                        child: const Text("添加"),
                      ),
                    ],
                  ),
                ),

                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
                  child: Align(
                    alignment: Alignment.centerLeft,
                    child: Text("共 ${_todos.length} 项，已完成 $doneCount 项"),
                  ),
                ),

                const SizedBox(height: 6),

                Expanded(
                  child: _todos.isEmpty
                      ? const Center(child: Text("暂无待办，先添加一条吧"))
                      : ListView.separated(
                          itemCount: _todos.length,
                          separatorBuilder: (_, __) => const Divider(height: 1),
                          itemBuilder: (context, index) {
                            final item = _todos[index];
                            return Dismissible(
                              key: ValueKey(item.id),
                              background: Container(
                                color: Colors.redAccent,
                                alignment: Alignment.centerRight,
                                padding: const EdgeInsets.only(right: 20),
                                child: const Icon(Icons.delete, color: Colors.white),
                              ),
                              direction: DismissDirection.endToStart,
                              onDismissed: (_) => _delete(index),
                              child: ListTile(
                                onTap: () => _toggle(index),
                                leading: Checkbox(
                                  value: item.done,
                                  onChanged: (_) => _toggle(index),
                                ),
                                title: Text(
                                  item.text,
                                  style: TextStyle(
                                    decoration: item.done
                                        ? TextDecoration.lineThrough
                                        : TextDecoration.none,
                                    color: item.done ? Colors.grey : null,
                                  ),
                                ),
                                trailing: IconButton(
                                  icon: const Icon(Icons.close),
                                  onPressed: () => _delete(index),
                                ),
                              ),
                            );
                          },
                        ),
                ),
              ],
            ),
      floatingActionButton: FloatingActionButton(
        onPressed: _add,
        child: const Icon(Icons.add),
      ),
    );
  }
}

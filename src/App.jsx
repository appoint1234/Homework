import { useEffect, useMemo, useState } from "react";
import ProfileCard from "./components/ProfileCard";
import { profiles, mottos } from "./data/profiles";

export default function App() {
  const [theme, setTheme] = useState("light"); // light | dark

  // 给 html 根节点加 data-theme，所有组件同步生效
  useEffect(() => {
    document.documentElement.setAttribute("data-theme", theme);
  }, [theme]);

  const themeText = useMemo(() => (theme === "light" ? "日间模式" : "夜间模式"), [theme]);

  return (
    <div className="page">
      <header className="header">
        <h1 className="title">名片夹 Profile Cards</h1>

        <button
          className="themeBtn"
          onClick={() => setTheme((t) => (t === "light" ? "dark" : "light"))}
        >
          切换：{themeText}
        </button>
      </header>

      <div className="grid">
        {profiles.map((p) => (
          <ProfileCard
            key={p.id}
            name={p.name}
            job={p.job}
            email={p.email}
            mottos={mottos}
          />
        ))}
      </div>
    </div>
  );
}

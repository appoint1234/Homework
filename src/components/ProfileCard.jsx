import { useMemo, useState } from "react";

export default function ProfileCard({ name, job, email, mottos }) {
  // 每张卡片独立座右铭
  const [motto, setMotto] = useState(mottos?.[0] ?? "Hello World!");

  // 防止每次渲染都重新生成“随机种子”
  const avatarText = useMemo(() => {
    const first = name?.trim()?.[0] ?? "?";
    return first.toUpperCase();
  }, [name]);

  const changeMotto = () => {
    if (!mottos || mottos.length === 0) return;

    // 随机选一条，尽量避免连续重复
    let next = mottos[Math.floor(Math.random() * mottos.length)];
    if (mottos.length > 1) {
      while (next === motto) {
        next = mottos[Math.floor(Math.random() * mottos.length)];
      }
    }
    setMotto(next);
  };

  return (
    <div className="card">
      <div className="avatar" aria-label="avatar">
        {avatarText}
      </div>

      <div className="info">
        <div className="name">{name}</div>
        <div className="job">{job}</div>
        <div className="email">{email}</div>

        <div className="motto">“{motto}”</div>

        <button className="btn" onClick={changeMotto}>
          随机更换座右铭
        </button>
      </div>
    </div>
  );
}

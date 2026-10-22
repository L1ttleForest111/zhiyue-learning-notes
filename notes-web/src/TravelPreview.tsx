import { useEffect, useState } from 'react';
import { Alert, Button, Spin } from 'antd';
import { api } from './api';

export default function TravelPreview({ noteId }: { noteId: number }) {
  const [previewUrl, setPreviewUrl] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [revision, setRevision] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    let cancelled = false;
    setLoading(true);
    setError('');
    setPreviewUrl('');
    api.getTravelMapPreview(noteId, controller.signal).then(document => {
      if (!cancelled) {
        const url = new URL(document.previewUrl);
        if (url.protocol !== 'http:' || url.hostname !== '127.0.0.1' || !url.port
            || url.username || url.password || url.search || url.hash
            || !/^\/preview\/[0-9a-f-]{36}$/.test(url.pathname)
            || url.origin === window.location.origin) {
          throw new Error('独立预览地址不合法，拒绝在主页面来源运行 HTML');
        }
        setPreviewUrl(url.href);
      }
    }).catch(cause => {
      if (!cancelled) { setError(cause instanceof Error ? cause.message : '攻略加载失败'); setLoading(false); }
    });
    return () => { cancelled = true; controller.abort(); };
  }, [noteId, revision]);

  useEffect(() => {
    if (!previewUrl || !loading) return;
    const timer = setTimeout(() => {
      setError('浏览器未完成攻略渲染，请重试。文字攻略不需要地图 Key。');
      setLoading(false);
      setPreviewUrl('');
    }, 20000);
    return () => clearTimeout(timer);
  }, [previewUrl, loading]);

  if (error) return <Alert type="error" showIcon title="攻略预览失败" description={error}
    action={<Button onClick={() => setRevision(value => value + 1)}>重新加载</Button>} />;

  return <Spin spinning={loading} description="正在加载攻略…">
    <div style={{ minHeight: 240 }}>
      {previewUrl && <iframe key={`${noteId}-${revision}`} className="travel-preview" title="隔离旅行攻略"
        src={previewUrl} sandbox="allow-scripts allow-same-origin" referrerPolicy="no-referrer"
        allow="geolocation 'none'; camera 'none'; microphone 'none'"
        onLoad={() => setLoading(false)} onError={() => { setError('浏览器无法加载攻略，请重试'); setLoading(false); }} />}
    </div>
  </Spin>;
}

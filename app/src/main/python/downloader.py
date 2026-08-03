import json
import os
import re
import time
from pathlib import Path


def _safe_name(value: str) -> str:
    value = re.sub(r'[\\/:*?"<>|]+', '_', value or 'instagram_video')
    value = re.sub(r'\s+', ' ', value).strip(' ._')
    return value[:78] or 'instagram_video'


def _write_cookie_file(cookie_header: str, target: Path) -> None:
    cookies = []
    for part in (cookie_header or '').split(';'):
        part = part.strip()
        if not part or '=' not in part:
            continue
        name, value = part.split('=', 1)
        name = name.strip()
        if name:
            cookies.append((name, value.strip()))

    if not any(name == 'sessionid' and value for name, value in cookies):
        raise RuntimeError('Сессия Instagram не найдена. Выполните вход внутри приложения.')

    lines = [
        '# Netscape HTTP Cookie File',
        '# Generated locally by Cardinalny Hvat / Cardinalich Software',
    ]
    for name, value in cookies:
        domain = '.instagram.com'
        if name in {'sessionid', 'ds_user_id'}:
            domain = '#HttpOnly_.instagram.com'
        lines.append(f'{domain}\tTRUE\t/\tTRUE\t2147483647\t{name}\t{value}')
    target.write_text('\n'.join(lines) + '\n', encoding='utf-8')


def download_instagram(url: str, cookie_header: str, output_dir: str, user_agent: str = '') -> str:
    result = {'ok': False}
    try:
        clean_url = (url or '').strip()
        if not re.match(r'^https?://([^/]+\.)?instagram\.com/', clean_url, re.I):
            raise ValueError('Нужна корректная ссылка Instagram.')

        from yt_dlp import YoutubeDL

        out_dir = Path(output_dir)
        out_dir.mkdir(parents=True, exist_ok=True)
        for old_file in out_dir.iterdir():
            if old_file.is_file():
                try:
                    old_file.unlink()
                except OSError:
                    pass

        cookie_file = out_dir / 'instagram.cookies.txt'
        _write_cookie_file(cookie_header, cookie_file)

        stamp = int(time.time())
        template = str(out_dir / f'grab_{stamp}_%(id)s.%(ext)s')
        headers = {
            'Referer': 'https://www.instagram.com/',
            'Accept-Language': 'ru-RU,ru;q=0.9,en;q=0.7',
        }
        if user_agent:
            headers['User-Agent'] = user_agent

        options = {
            'cookiefile': str(cookie_file),
            'outtmpl': template,
            'format': 'best[ext=mp4]/best',
            'noplaylist': True,
            'quiet': True,
            'no_warnings': True,
            'restrictfilenames': False,
            'http_headers': headers,
            'overwrites': True,
            'retries': 3,
            'fragment_retries': 3,
            'socket_timeout': 25,
            'concurrent_fragment_downloads': 1,
        }

        with YoutubeDL(options) as ydl:
            info = ydl.extract_info(clean_url, download=True)
            if not info:
                raise RuntimeError('Instagram не вернул данные публикации.')

            requested = info.get('requested_downloads') or []
            path = requested[0].get('filepath') if requested else None
            if not path:
                path = ydl.prepare_filename(info)
            if not path or not os.path.exists(path):
                candidates = [p for p in out_dir.glob(f'grab_{stamp}_*') if p.is_file()]
                candidates.sort(key=lambda p: p.stat().st_mtime, reverse=True)
                if candidates:
                    path = str(candidates[0])
            if not path or not os.path.exists(path):
                raise RuntimeError('Загрузчик завершился, но видеофайл не найден.')

            title = _safe_name(
                info.get('title')
                or info.get('description')
                or info.get('uploader')
                or 'Instagram video'
            )
            ext = Path(path).suffix.lower() or '.mp4'
            result = {
                'ok': True,
                'path': os.path.abspath(path),
                'title': title,
                'filename': f'{title}{ext}',
            }
    except Exception as exc:
        result = {
            'ok': False,
            'error': str(exc) or exc.__class__.__name__,
        }
    return json.dumps(result, ensure_ascii=False)

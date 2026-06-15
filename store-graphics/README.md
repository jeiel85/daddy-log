# 두피케어기록 Store Graphics

Play Console 등록정보에 바로 사용할 그래픽 자료입니다.

| 파일 | 용도 | 규격 |
| --- | --- | --- |
| `icon-512.png` | 앱 아이콘 | 512x512 PNG |
| `feature-graphic-1024x500.png` | 기능 그래픽 | 1024x500 PNG |
| `play-console-current/` | 현재 업로드 묶음 | 아이콘, 기능 그래픽, 릴리즈 노트, 스크린샷 폴더 |

모든 그래픽은 코드로 생성합니다. 브랜드 색상이나 디자인을 바꿀 때는 아래 스크립트를 다시 실행하세요.

```powershell
python scripts/gen_icon.py      # 앱 아이콘 · 스토어 아이콘 · 파비콘
python scripts/gen_graphics.py  # 기능 그래픽 · OG 카드 · 랜딩 이미지
```

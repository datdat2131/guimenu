# AstraxMenu 1.0.0 – Menu GUI giới thiệu, hướng dẫn và /rank cho BoxMine RPG

Paper **1.21.4**, **Java 21**, Maven. Cùng phong cách với AstraxElementer / AstraxQuest / AstraxTitles / AstraxTeam / NguyenTo.

> **Trạng thái trung thực:** mã nguồn đầy đủ, đã kiểm tra cú pháp Java và YAML (liên kết menu, ô, độ dài trang sách), nhưng **chưa build bằng Maven với Paper API thật và chưa chạy trên server**. Hãy build (GitHub Actions có sẵn trong `.github/workflows/build.yml`) và thử trên server test; nếu lỗi biên dịch, gửi log `mvn` để sửa.

## Có gì
- **`/menu`** (alias `/huongdan`, `/guide`): menu chính giới thiệu server, lối tắt tới `/quest`, `/title`, `/elementer`, `/nt`, `/team`, `/kit`, `/rank`, nội quy, Discord, Website.
- **Hướng dẫn hệ thống:** menu hướng dẫn cho Tân thủ, Nguyên Tố RPG (AstraxElementer), Nguyên Tố Bay (NguyenTo), Nhiệm vụ, Danh hiệu, Team. Mỗi trang có **sách hướng dẫn** đọc đầy đủ (`guides.yml`) và nút mở thẳng hệ thống đó. Có thêm menu **Danh sách lệnh**.
- **`/rank`** (alias `/ranks`, `/quyenloi`): xem quyền lợi từng rank.
  - Chuột trái vào rank: **xem trước vật phẩm thật trong `/kit` của rank đó** (plugin đọc `kits.yml` của EssentialsX, chỉ đọc, không sửa).
  - Chuột phải vào rank: chạy `/kit <tên kit>` để nhận (chỉ khi bạn có rank đó).
  - Hiện rank hiện tại của người chơi, trạng thái từng rank, thời gian hồi kit, cách nhận rank.
  - `/rank <id>` mở thẳng một rank; `/rank list` in danh sách ra chat.
- **Vật phẩm mở menu** (tùy chọn): Sao Địa Ngục ở ô hotbar, chuột phải mở menu.
- **Tự mở menu cho người chơi vào lần đầu** + nhắc `/menu`, `/rank` khi vào server.
- Mọi thứ cấu hình bằng file YAML, `/astraxmenu reload` là xong.

## Cài đặt
1. `mvn clean package` → `target/AstraxMenu-1.0.0.jar` (hoặc đẩy lên GitHub, tab Actions tự build).
2. Chép vào `plugins/`, khởi động lại. Plugin tự tạo `plugins/AstraxMenu/` gồm `config.yml`, `menus.yml`, `ranks.yml`, `guides.yml`, `messages.yml`.
3. **Việc bạn cần làm:** mở `ranks.yml` sửa rank/perk cho đúng server (bản mẫu là vip1…vipmax, **đây chỉ là mẫu**), đổi link Discord/Website trong `menus.yml` (tìm `doi-link`), chỉnh nội quy mẫu.
4. Gõ `/astraxmenu check` để kiểm tra: có đọc được kit không, rank nào thiếu kit, menu nào trỏ sai.

Mềm phụ thuộc (không bắt buộc): PlaceholderAPI, Essentials/EssentialsX (đọc kit), LuckPerms.

## Cấu hình /rank (quan trọng)
Rank của bạn làm bằng `/kit`, nên mỗi rank trong `ranks.yml` có:

| Khóa | Ý nghĩa |
|---|---|
| `kit` | tên kit trong `/kit` (khớp tên trong `kits.yml`) |
| `permission` | quyền để coi là "đang có rank". Mặc định `group.<id>` (LuckPerms). Nếu bạn phân quyền kit bằng `essentials.kits.vip1` thì đặt đúng quyền đó |
| `name`, `icon`, `price` | tên, biểu tượng, cách nhận rank |
| `description`, `perks` | chữ bạn tự viết: quyền lợi ngoài kit (fly, home, prefix...) |

Thứ tự trong file = thấp → cao; "rank hiện tại" = rank cao nhất người chơi đang có. Ô hiển thị từng rank ở `settings.slots`. Đổi lệnh nhận kit ở `settings.kit-command` (vd `ekit {kit}`).

Lưu ý: kiểm tra rank dùng `isPermissionSet && hasPermission` nên **OP không bị coi là có mọi rank**. Xem trước kit hiểu được dòng item Essentials dạng `diamond_sword 1 sharpness:5 name:&bKiếm`; dòng lệnh (`/give ...`) được gộp vào "phần thưởng đặc biệt".

## Làm menu của riêng bạn
`menus.yml`: thêm một khối dưới `menus:`, mở bằng `/menu <id>` hoặc `[open] <id>` từ nút khác. Danh sách hành động và biến nằm ở đầu file. Ví dụ nút chạy lệnh và mở sách:
```yaml
      shop:
        slot: 22
        material: EMERALD
        name: "&a&lCửa hàng"
        lore: ["&7Nhấp để mở"]
        actions: ["[close]", "[player] shop"]
```
Sách hướng dẫn mới: thêm vào `guides.yml` (mỗi `-` một trang) rồi dùng `[book] <id>`.

## Lệnh và quyền
| Lệnh | Quyền | Mặc định |
|---|---|---|
| `/menu [menu]` | `astraxmenu.use` | mọi người |
| `/rank [id\|list]` | `astraxmenu.rank` | mọi người |
| `/astraxmenu reload\|list\|check` | `astraxmenu.admin` | op |
| `/astraxmenu open <người> <menu>` | `astraxmenu.admin` | op |

## Lưu ý tương thích giữa các plugin của bạn
- **Trùng lệnh `/nguyento`:** `AstraxElementer` có alias `nguyento` còn plugin `NguyenTo` có lệnh chính `nguyento`. Menu này dùng `/elementer` cho Elementer và `/nt` cho NguyenTo để tránh nhầm. Nên xóa alias `nguyento` (và `eadmin` nếu trùng) trong `plugin.yml` của AstraxElementer.
- Nội dung hướng dẫn viết theo cấu hình **mặc định** của các plugin (đọc từ README/config bạn gửi). Nếu bạn đổi số liệu (số Tinh Thể, cấp mở hào quang...), sửa lại trong `guides.yml`/`menus.yml`.
- Plugin không tự ghi đè file YAML đã có khi cập nhật. Muốn lấy bản mới: đổi tên file cũ rồi restart.

## Giới hạn đã biết
- Chưa build với Paper API thật (xem đầu file).
- Xem trước kit chỉ hỗ trợ định dạng `kits.yml` của EssentialsX. Dùng plugin kit khác thì đặt `show-kit-contents: false` (vẫn có chữ `perks` và nút nhận kit).
- Tên vật phẩm trong xem trước hiển thị theo ngôn ngữ client.
- Kit không có trong `kits.yml` thì hiện thông báo "không đọc được kit" thay vì lỗi.

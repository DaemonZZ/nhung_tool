# Nhật Ký Phát Triển App Đối Chiếu XNT - Hóa Đơn

## 1. Mục tiêu app

App được xây dựng để tự động đối chiếu dữ liệu giữa:

1. File `XNT` nội bộ
2. File hóa đơn đầu vào `MuaVao_1`
3. File hóa đơn đầu ra `BanRa_1`

Kết quả cần phục vụ:

1. Đối chiếu nhập, xuất, tồn theo cả `số lượng` và `thành tiền`
2. Phát hiện chênh lệch giữa hóa đơn và XNT
3. Phát hiện `âm kho`
4. Cảnh báo `lệch đơn vị tính`
5. Xử lý các mặt hàng `không có trong XNT` nhưng vẫn xuất hiện trên hóa đơn

## 2. Bộ tài liệu và dữ liệu gốc đã dùng

Nguồn đầu vào ban đầu:

1. `docs/ord tool.docx`: mô tả yêu cầu nghiệp vụ
2. `docs/xnt.xlsx`: dữ liệu XNT mẫu
3. `docs/chi tiet hoa don anh huy hoang.xlsx`: dữ liệu hóa đơn mẫu
4. `docs/detail.xlsx`: mẫu output tham chiếu

Tài liệu nội bộ đã tạo trong quá trình làm:

1. `docs/mo-ta-chi-tiet-tool-doi-chieu.md`
2. `docs/prompt-thiet-ke-ui.md`
3. `docs/ui-design-prompt-en.md`
4. `docs/design-ai-prompt.txt`
5. Các prompt review/chỉnh sửa design AI trong thư mục `docs`
6. `docs/huong-dan-su-dung-app.md`

## 3. Các mốc phát triển chính

### Giai đoạn 1: Đọc yêu cầu và chốt concept nghiệp vụ

Đã đọc file yêu cầu gốc và đối chiếu với dữ liệu mẫu để hiểu bài toán.

Concept đã chốt:

1. `XNT` là nguồn chuẩn cho tồn đầu kỳ, nhập, xuất, tồn cuối kỳ
2. Hóa đơn là dữ liệu phát sinh để đối chiếu với XNT
3. Kết quả cần có ít nhất 2 nhóm:
   - `KetQua_ChiTiet`
   - `KetQua_AmKho`

### Giai đoạn 2: Làm rõ rule nghiệp vụ với user

Các rule đã chốt:

1. Output phải so sánh cả `số lượng` và `thành tiền`
2. Ưu tiên match theo `tên hàng`
3. Có thể dùng `model AI nhỏ` cho các case gợi ý match mơ hồ trong tương lai
4. `Sai lệch ĐVT` hiện tại chỉ là `cảnh báo`, chưa chặn xử lý
5. Hàng có trên hóa đơn nhưng `không có trong XNT` vẫn phải có trong kết quả
6. Các token số như `model`, `kích thước`, `độ dày`, `quy cách`, `đường kính` là token quan trọng, không được match lỏng
7. Có thể nới match ở phần chữ như chuẩn hóa dấu, khoảng trắng, cách viết

Điểm còn mở:

1. Thứ tự xử lý `mua` và `bán` trong cùng một ngày khi tính âm kho chưa được chốt nghiệp vụ cuối

### Giai đoạn 3: Viết tài liệu đặc tả nghiệp vụ

Đã tạo tài liệu mô tả chi tiết để cố định yêu cầu:

1. Mục tiêu nghiệp vụ
2. Cấu trúc input/output
3. Rule match tên hàng
4. Rule cảnh báo lệch ĐVT
5. Xử lý hàng ngoài XNT
6. Cấu trúc sheet đầu ra

File kết quả:

1. `docs/mo-ta-chi-tiet-tool-doi-chieu.md`

### Giai đoạn 4: Thiết kế UI bằng AI và review nhiều vòng

Đã thực hiện:

1. Tạo prompt thiết kế UI bằng tiếng Việt
2. Tạo prompt thiết kế UI bằng tiếng Anh
3. Xuất prompt ra file text để copy trực tiếp vào design AI
4. Review nhiều vòng bộ design trong thư mục `design`
5. Viết prompt chỉnh sửa sau từng vòng review

Mục tiêu của giai đoạn này:

1. Chốt structure UI trước khi code JavaFX/FXML
2. Đảm bảo design bám đúng nghiệp vụ đối chiếu kho/thuế
3. Loại bỏ các sample screen generic không đúng domain

### Giai đoạn 5: Scaffold project Kotlin + JavaFX + FXML

Đã tạo project chạy được với:

1. `Kotlin`
2. `Gradle`
3. `JavaFX`
4. `FXML`

Đã có:

1. Entry point app
2. Main shell
3. Sidebar navigation
4. CSS chung
5. Bộ screen skeleton theo design

### Giai đoạn 6: Dựng UI skeleton theo thiết kế

Đã dựng các màn:

1. `Workspace Dashboard`
2. `Input / Validation`
3. `Configuration`
4. `Execution Progress`
5. `Mapping Review`
6. `Unit Review`
7. `Detailed Results`
8. `Negative Inventory`
9. `Export`
10. `Run History`
11. `Settings`

Ở giai đoạn này dữ liệu ban đầu chủ yếu là `dummy/sample`.

### Giai đoạn 7: Chuyển màn đầu sang dữ liệu thật

Đã nối `Dashboard` với dữ liệu thật ở mức:

1. Tên file
2. Metadata file
3. Danh sách sheet
4. Nhận diện period cơ bản
5. Validation cơ bản về file và sheet

Đây là bước chuyển từ UI tĩnh sang UI có đọc workbook thật.

### Giai đoạn 8: Tích hợp pipeline phân tích dữ liệu thật cho toàn app

Đã tạo `WorkspaceAnalysisService` làm service trung tâm để:

1. Đọc `xnt.xlsx`
2. Đọc `MuaVao_1`
3. Đọc `BanRa_1`
4. Parse dữ liệu số lượng, thành tiền, ngày chứng từ
5. Normalize tên hàng và đơn vị
6. Match hàng giữa hóa đơn và XNT
7. Sinh dữ liệu cho các màn review, kết quả, progress, export preview

Từ đây các màn dữ liệu chính đã bỏ `SampleData` và chuyển sang dùng `real data`.

### Giai đoạn 9: Bổ sung chọn file đầu vào và kiểm tra format trước khi nạp

Đã bổ sung `WorkspaceInputService` để quản lý:

1. File `active` đang dùng trong workspace
2. File `pending` user vừa chọn nhưng chưa nạp
3. Kết quả `validate format` của file đang chọn

Các khả năng đã có:

1. Chọn file `XNT`
2. Chọn file workbook hóa đơn
3. Kiểm tra format trước khi nạp
4. Chỉ cho phép nạp khi format hợp lệ
5. Giữ nguyên dữ liệu `active` nếu file `pending` chưa pass validation

Validation hiện tại kiểm tra:

1. Loại file Excel hỗ trợ
2. Sheet bắt buộc
3. Header/cột bắt buộc
4. Cấu trúc nhóm cột lượng/tiền của XNT
5. Số dòng dữ liệu cơ bản

### Giai đoạn 10: Hoàn thiện màn Input / Validation theo dữ liệu thật

Màn `Input / Validation` hiện đã được nâng cấp để:

1. Hiển thị thông tin file `active` và `pending`
2. Hiển thị metadata thật của file
3. Hiển thị warning/error format thật
4. Hiển thị số dòng thật của từng sheet dữ liệu
5. Hiển thị toàn bộ dòng dữ liệu thật, không còn giới hạn `50 dòng preview`
6. Tô màu các dòng có warning
7. Filter chỉ hiện các dòng có warning

Điểm đã sửa thêm sau khi QA:

1. Đồng bộ lại `summary` với dataset thực tế
2. Loại bỏ mismatch giữa `warning count` và kết quả filter
3. Tách warning theo `XNT`, `MuaVao`, `BanRa` rõ hơn

### Giai đoạn 11: Hoàn thiện workflow thật cho màn Mapping Review

Màn `Mapping Review` đã được nâng cấp từ màn chỉ xem thành workflow có tác động thật tới engine.

Các chức năng đã có:

1. Search/filter theo tên invoice, mã/tên XNT, warning, trạng thái
2. `Confirm Match`
3. `Remap` sang catalog XNT thật
4. `Mark Not in XNT`
5. `Ignore Warning` và `Restore Warning`
6. Bulk actions cho nhiều dòng
7. Tô màu các dòng `pending review`, `manual decision`, `warning`
8. Lưu quyết định mapping của user ra file cục bộ
9. Tính lại các màn kết quả dựa trên quyết định mapping mới

### Giai đoạn 12: Bổ sung Undo cho Mapping Review

Đã bổ sung cơ chế `undo` cho màn `Mapping Review`.

Undo hiện tại:

1. Hoàn tác được `tác vụ gần nhất`
2. Hỗ trợ cả action đơn lẻ và bulk action
3. Sau khi undo, app refresh lại shell và tính lại toàn bộ state liên quan
4. Undo history hiện là `in-memory` theo session đang chạy

## 4. Trạng thái hiện tại của app

### 4.1. Những phần đã chạy trên dữ liệu thật

1. `Dashboard`
2. `Input / Validation`
3. `Mapping Review`
4. `Unit Review`
5. `Detailed Results`
6. `Negative Inventory`
7. `Execution Progress`
8. `Run History`
9. `Export Preview`

### 4.2. Những gì app đang làm thật

1. Đọc workbook Excel thật
2. Parse dữ liệu XNT và hóa đơn
3. Match tên hàng theo heuristic an toàn
4. Tính kết quả đối chiếu lượng và tiền
5. Phát hiện cảnh báo lệch ĐVT
6. Phát hiện các thời điểm âm kho
7. Sinh preview cho output workbook
8. Cho user chọn file đầu vào thật
9. Kiểm tra format file đầu vào trước khi nạp
10. Hiển thị và filter warning trên màn input
11. Cho user chốt quyết định mapping thật
12. Lưu quyết định mapping cục bộ
13. Hỗ trợ undo tác vụ mapping gần nhất

### 4.3. Những gì vẫn chưa hoàn chỉnh

1. `Unit Review` chưa thành workflow thao tác thật ở mức tương đương `Mapping Review`
2. `Export Workbook` chưa xuất file thật theo cấu trúc cuối cùng
3. `Redo` chưa có cho `Mapping Review`
4. Undo history chưa được lưu qua lần mở app sau
5. Rule âm kho cùng ngày vẫn đang dùng assumption tạm
6. `Configuration` hiện vẫn chủ yếu là UI placeholder, chưa điều khiển engine thật

## 5. Kiến trúc kỹ thuật hiện tại

### 5.1. Stack

1. Kotlin
2. Gradle
3. JavaFX
4. FXML
5. Apache POI để đọc Excel

### 5.2. Luồng dữ liệu hiện tại

1. `WorkspaceInputService` quản lý file `pending`, file `active`, và validation format
2. Khi user nạp file hợp lệ, `WorkspaceAnalysisService` đọc file `active`
3. Parse ra dữ liệu XNT, mua vào, bán ra
4. Chạy normalize và match
5. `MappingDecisionService` áp quyết định mapping của user nếu có
6. Tạo view model dùng chung cho toàn bộ màn hình
7. Controller các màn lấy dữ liệu từ cùng một state phân tích
8. `AppUiCoordinator` dùng để refresh lại shell và screen sau khi đổi nguồn dữ liệu hoặc đổi mapping

### 5.3. Nguyên tắc match hiện tại

Thứ tự match:

1. `Confirmed` hoặc `Remapped` nếu user đã có quyết định
2. `Exact`
3. `Normalized`
4. `Heuristic`
5. `Needs review`
6. `Not in XNT`

Nguyên tắc an toàn:

1. Không cố ép match khi confidence thấp
2. Token số/model được giữ chặt
3. Lệch ĐVT sinh warning
4. Nếu không đủ chắc thì đưa vào review thay vì tự động merge

## 6. Các quyết định kỹ thuật quan trọng đã chốt

1. Không tiếp tục dùng `SampleData` cho các màn nghiệp vụ chính
2. Dùng một service phân tích trung tâm thay vì để mỗi controller tự đọc file
3. Match tên hàng ưu tiên an toàn, không thiên về auto-merge
4. Cho phép hàng ngoài XNT vẫn lên report
5. Dùng `real workbook` làm data source, với bộ file trong `docs` là mặc định khởi tạo ban đầu
6. Tách `file đang chọn` và `file đang dùng` để tránh nạp nhầm dữ liệu lỗi format
7. Màn input phải hiển thị và lọc warning trên chính dataset thật đang thấy
8. Quyết định mapping của user phải được ưu tiên hơn gợi ý tự động của engine
9. Quyết định mapping được lưu cục bộ ra file để giữ lại qua lần mở app sau
10. `Undo` cho mapping hiện được hỗ trợ theo session đang chạy

## 7. Rủi ro và điểm còn mở

1. Rule âm kho cùng ngày chưa chốt nghiệp vụ cuối
2. Match heuristic hiện tại là bản đầu, chưa có bước học từ quyết định user
3. Chưa có bảng quy đổi đơn vị để xử lý các case `kg/cây`, `bao/tấn`, `m/cuộn`
4. Chưa có cơ chế lưu full review session và mở lại theo từng lần chạy
5. Chưa có kiểm thử tự động cho engine parse/match/reconcile
6. Chưa có `redo` sau `undo`

## 8. Việc nên làm tiếp theo

### Ưu tiên cao

1. Làm workflow review thật cho `Unit Review`
2. Xuất file Excel thật theo mẫu `detail.xlsx`
3. Chốt rule âm kho trong ngày với user
4. Nối `Configuration` vào engine thật
5. Bổ sung `Redo` cho `Mapping Review`

### Ưu tiên tiếp theo

1. Thêm cấu hình quy đổi đơn vị
2. Bổ sung history/run session thật
3. Viết test cho parser và reconciliation engine
4. Cân nhắc tối ưu hiệu năng hiển thị nếu dữ liệu đầu vào lớn hơn bộ mẫu hiện tại
5. Xem xét lưu undo/redo history xuống file nếu cần

## 9. Trạng thái tổng kết tại thời điểm ghi file

App đã qua giai đoạn:

1. đọc yêu cầu
2. dựng design
3. scaffold project
4. dựng UI
5. nối dữ liệu thật
6. thêm chọn file và validate format
7. hoàn thiện màn input theo warning thật
8. hoàn thiện workflow thật cho màn mapping
9. bổ sung undo cho mapping

Trạng thái hiện tại:

1. `UI đã chạy`
2. `Dữ liệu thật đã được đọc từ file active`
3. `User đã có thể chọn file và validate format trước khi nạp`
4. `Màn input đã hiển thị warning thật, filter warning và tô màu dòng lỗi`
5. `Mapping Review` đã có action thật, lưu quyết định và undo
6. `Kết quả đối chiếu đã có preview`
7. `Các action nghiệp vụ cuối chưa hoàn tất`

Nói ngắn gọn: app đã sang giai đoạn `working prototype with real data`, chưa phải `production-complete workflow`.

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
7. `docs/nhat-ky-phat-trien-app.md`

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
3. Danh sách tab
4. Nhận diện period cơ bản
5. Validation cơ bản về file và tab

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
2. Tab bắt buộc
3. Header/cột bắt buộc
4. Cấu trúc nhóm cột lượng/tiền của XNT
5. Số dòng dữ liệu cơ bản

### Giai đoạn 10: Hoàn thiện màn Input / Validation theo dữ liệu thật

Màn `Input / Validation` hiện đã được nâng cấp để:

1. Hiển thị thông tin file `active` và `pending`
2. Hiển thị metadata thật của file
3. Hiển thị warning/error format thật
4. Hiển thị số dòng thật của từng tab dữ liệu
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

### Giai đoạn 13: Hoàn thiện workflow thật cho Unit Review

Màn `Unit Review` đã được nâng cấp thành workflow thật thay vì chỉ hiển thị danh sách warning.

Các chức năng đã có:

1. `Confirm Resolution`
2. `Keep Warning`
3. `Remap Item`
4. `Reset Review`
5. Bulk action cho nhiều dòng
6. `Undo` cho action gần nhất của riêng màn unit
7. `Reset All Reviews` để đưa toàn bộ review state về trạng thái ban đầu
8. Lưu quyết định review đơn vị ra file cục bộ
9. Áp quyết định review đơn vị vào `Detailed Results`, `Negative Inventory`, `Dashboard`, `Export`

### Giai đoạn 14: Hoàn thiện Export Workbook thật

Màn `Export` đã được nối vào service xuất file Excel thật.

Các khả năng hiện có:

1. Chọn thư mục xuất
2. Đặt tên file xuất
3. Chọn `Export all data` hoặc `Export reviewed only`
4. Chọn kèm tab cảnh báo
5. Chọn kèm nhật ký chạy
6. Tạo file `.xlsx` thật từ dữ liệu đang active trong workspace

Workbook đầu ra hiện có:

1. `KetQua_ChiTiet`
2. `KetQua_AmKho`
3. `CanhBao_AnhXa` nếu bật
4. `CanhBao_DonVi` nếu bật
5. `NhatKy_Chay` nếu bật

### Giai đoạn 15: Quy hoạch lại UI các màn review và kết quả

Đã tối ưu lại layout cho các màn có mật độ dữ liệu cao:

1. `Mapping Review`
2. `Unit Review`
3. `Detailed Results`
4. `Negative Inventory`
5. `Dashboard`

Các nguyên tắc đã áp dụng:

1. Tách header thành nhiều tầng rõ ràng
2. Tách search, filter, bulk actions
3. Dùng `FlowPane` cho cụm nút dài để tránh cắt chữ
4. Tăng chiều rộng cột quan trọng
5. Dùng tooltip cho text dài trong bảng
6. Tăng khả năng đọc trên màn hình laptop

### Giai đoạn 16: Việt hóa toàn bộ app

Toàn bộ ngôn ngữ hiển thị cho người dùng trong app đã được chuyển sang tiếng Việt.

Phạm vi đã đổi:

1. Navigation và top bar
2. Tên các màn hình
3. Nút thao tác, filter, summary, badge
4. Dialog xác nhận, cảnh báo, thông báo lỗi/thành công
5. Log tiến trình và preview export

Các định danh nghiệp vụ được giữ nguyên có chủ đích:

1. `XNT`
2. `MuaVao_1`
3. `BanRa_1`
4. `KetQua_ChiTiet`
5. `KetQua_AmKho`

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
10. `Export Workbook`

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
14. Cho user chốt quyết định review đơn vị thật
15. Lưu quyết định unit review cục bộ
16. Hỗ trợ undo tác vụ unit review gần nhất
17. Cho user reset toàn bộ review state về trạng thái ban đầu
18. Xuất file Excel thật từ UI export
19. Hiển thị toàn bộ text UI bằng tiếng Việt

### 4.3. Những gì vẫn chưa hoàn chỉnh

1. `Configuration` hiện vẫn chủ yếu là UI placeholder, chưa điều khiển engine thật
2. Rule âm kho cùng ngày vẫn đang dùng assumption tạm
3. Export đã ra file thật nhưng chưa được format sát mẫu `detail.xlsx` cuối cùng
4. `Run History` mới là dữ liệu phiên hiện tại, chưa có persistence thật cho nhiều lần chạy/export
5. Undo history chưa được lưu qua lần mở app sau
6. Chưa có cơ chế quy đổi đơn vị thật cho các case khác đơn vị nhưng cùng mặt hàng

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
6. `UnitReviewDecisionService` áp quyết định review đơn vị của user nếu có
7. `ExportWorkbookService` dùng state phân tích hiện tại để sinh workbook thật
8. Tạo view model dùng chung cho toàn bộ màn hình
9. Controller các màn lấy dữ liệu từ cùng một state phân tích
10. `AppUiCoordinator` dùng để refresh lại shell và screen sau khi đổi nguồn dữ liệu hoặc đổi review state

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
11. Quyết định review đơn vị cũng được lưu cục bộ và có undo riêng theo session
12. App phải cho user khôi phục toàn bộ review state về trạng thái ban đầu ngay trong UI
13. Export workbook phải đọc lại toàn bộ quyết định mapping/unit review hiện hành
14. Toàn bộ UI hiển thị theo tiếng Việt, chỉ giữ nguyên các định danh nghiệp vụ Excel

## 7. Rủi ro và điểm còn mở

1. Rule âm kho cùng ngày chưa chốt nghiệp vụ cuối
2. Match heuristic hiện tại là bản đầu, chưa có bước học từ quyết định user
3. Chưa có bảng quy đổi đơn vị để xử lý các case `kg/cây`, `bao/tấn`, `m/cuộn`
4. Chưa có cơ chế lưu full review session và mở lại theo từng lần chạy
5. Chưa có kiểm thử tự động cho engine parse/match/reconcile
6. Export workbook hiện đúng nghiệp vụ chính nhưng chưa hoàn thiện format trình bày cuối cùng như file mẫu khách hàng

## 8. Việc nên làm tiếp theo

### Ưu tiên cao

1. Chốt rule âm kho trong ngày với user
2. Nối `Configuration` vào engine thật
3. Hoàn thiện format file Excel đầu ra để sát mẫu khách hàng hơn
4. Bổ sung persistence thật cho `Run History`
5. Bổ sung test cho parser, matching và reconciliation engine

### Ưu tiên tiếp theo

1. Thêm cấu hình quy đổi đơn vị
2. Cân nhắc tối ưu hiệu năng hiển thị nếu dữ liệu đầu vào lớn hơn bộ mẫu hiện tại
3. Xem xét lưu undo history xuống file nếu cần
4. Cân nhắc học dần heuristic từ quyết định user
5. Rà lại toàn bộ wording nghiệp vụ sau các vòng QA tiếp theo

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
10. hoàn thiện workflow thật cho unit review
11. bổ sung undo và reset toàn bộ review state
12. xuất workbook thật
13. tối ưu layout cho các màn review/kết quả/dashboard
14. Việt hóa toàn bộ app

Trạng thái hiện tại:

1. `UI đã chạy`
2. `Dữ liệu thật đã được đọc từ file active`
3. `User đã có thể chọn file và validate format trước khi nạp`
4. `Màn input đã hiển thị warning thật, filter warning và tô màu dòng lỗi`
5. `Mapping Review` đã có action thật, lưu quyết định và undo
6. `Unit Review` đã có action thật, lưu quyết định và undo
7. `Export` đã xuất được file Excel thật
8. `Các màn chính đã được tối ưu layout để giảm cắt chữ`
9. `Toàn bộ app đã dùng tiếng Việt ở phần hiển thị cho người dùng`
10. `Configuration`, rule âm kho trong ngày, persistence lịch sử chạy và format Excel cuối vẫn cần hoàn thiện`

Nói ngắn gọn: app đã sang giai đoạn `working prototype with real data and executable export`, chưa phải `production-complete workflow`.

# Prompt thiết kế UI cho tool đối chiếu XNT - hóa đơn

## Prompt chính

Thiết kế một bộ UI/UX hoàn chỉnh cho một ứng dụng desktop nội bộ viết bằng `Kotlin + JavaFX + FXML`, dùng để tự động đối chiếu số liệu giữa:

- file `XNT` nội bộ
- file hóa đơn `Mua vào`
- file hóa đơn `Bán ra`

Mục tiêu của tool:

1. So sánh số liệu `số lượng` và `thành tiền` giữa hóa đơn và XNT.
2. Phát hiện chênh lệch nhập, xuất, tồn cuối kỳ.
3. Phát hiện các thời điểm `âm kho`.
4. Cảnh báo các trường hợp `lệch đơn vị tính`.
5. Hỗ trợ nhận diện/match tên hàng giữa XNT và hóa đơn theo nhiều mức:
   - khớp chính xác
   - khớp sau normalize
   - khớp theo AI gợi ý
   - không có trong XNT
   - cần người dùng review
6. Hỗ trợ xử lý các mặt hàng có trên hóa đơn nhưng không có trong XNT.
7. Cho phép người dùng review các cảnh báo và thực hiện action thủ công trước khi xuất kết quả cuối cùng.

Yêu cầu thiết kế một UI chuẩn, đầy đủ chức năng, thiên về `desktop productivity app`, rõ ràng, tốc độ thao tác cao, phù hợp cho người dùng nghiệp vụ kế toán/thuế/kho, không mang phong cách marketing, không làm kiểu dashboard web chung chung.

## Bối cảnh nghiệp vụ cần phản ánh trong UI

Ứng dụng xử lý 3 file đầu vào:

1. `xnt.xlsx`: dữ liệu chuẩn, chứa danh mục hàng hóa, đơn vị tính, tồn đầu kỳ, nhập, xuất, tồn cuối kỳ.
2. file hóa đơn chi tiết có 2 sheet:
   - `MuaVao`
   - `BanRa`

Ứng dụng sinh ra 2 nhóm kết quả:

1. `Kết quả chi tiết`
   - đối chiếu theo mặt hàng
   - so sánh cả số lượng và thành tiền
   - có trạng thái match và cảnh báo
2. `Kết quả âm kho`
   - chỉ hiển thị các thời điểm bị âm kho
   - cho thấy tồn đầu kỳ, lũy kế mua, lũy kế bán, lượng âm kho, giá trị bán trong ngày

Một số rule quan trọng:

1. Ưu tiên match theo `tên hàng`, không phụ thuộc hoàn toàn vào mã hàng.
2. Phần `số/model/kích thước/quy cách` trong tên hàng là tín hiệu mạnh, không được gộp sai.
3. Sai khác đơn vị tính chỉ `cảnh báo`, không chặn xử lý.
4. Các mặt hàng có trên hóa đơn nhưng không có trong XNT vẫn phải xuất hiện trong kết quả.
5. Có các case cần người dùng review thủ công trước khi chốt kết quả.

## Mục tiêu UI

Thiết kế UI phải giúp người dùng hoàn thành trọn vẹn quy trình sau:

1. Chọn và nạp file đầu vào.
2. Kiểm tra nhanh chất lượng dữ liệu đầu vào.
3. Chạy đối chiếu.
4. Xem tổng quan kết quả.
5. Review các cảnh báo và các match không chắc chắn.
6. Chỉnh sửa/confirm mapping nếu cần.
7. Xem bảng kết quả chi tiết.
8. Xem bảng kết quả âm kho.
9. Lọc, tìm kiếm, nhóm, sort dữ liệu.
10. Xuất file kết quả cuối cùng.
11. Theo dõi lịch sử xử lý/log chạy.

## Yêu cầu về cấu trúc màn hình

Hãy thiết kế đầy đủ các màn hình hoặc panel chính sau:

1. `Màn hình chính / Workspace`
2. `Panel nhập file đầu vào`
3. `Panel kiểm tra dữ liệu đầu vào`
4. `Panel cấu hình xử lý`
5. `Panel tiến trình chạy`
6. `Màn hình tổng quan kết quả`
7. `Màn hình review mapping mặt hàng`
8. `Màn hình review cảnh báo đơn vị tính`
9. `Màn hình kết quả chi tiết`
10. `Màn hình kết quả âm kho`
11. `Màn hình export`
12. `Màn hình lịch sử chạy / log`
13. `Màn hình settings`

## Yêu cầu chi tiết theo màn hình

### 1. Màn hình chính / Workspace

Thiết kế một màn hình desktop chính theo bố cục chuyên nghiệp, ưu tiên năng suất:

- top bar với tên tool, kỳ dữ liệu đang xử lý, trạng thái file
- left navigation hoặc sidebar cho các module
- vùng nội dung chính linh hoạt theo tab hoặc split-pane
- bottom status bar hiển thị trạng thái hệ thống, số cảnh báo, tiến độ, thời gian xử lý gần nhất

Các module trên sidebar nên có:

- Nhập liệu
- Kiểm tra dữ liệu
- Chạy đối chiếu
- Review mapping
- Cảnh báo
- Kết quả chi tiết
- Âm kho
- Xuất file
- Lịch sử
- Cài đặt

### 2. Panel nhập file đầu vào

UI phải cho phép:

- chọn file `XNT`
- chọn file hóa đơn chi tiết
- tự nhận diện sheet `MuaVao` và `BanRa`
- hiển thị trạng thái file đã nạp/chưa nạp
- hiển thị metadata như tên file, ngày sửa cuối, số dòng, số sheet
- nút replace file, clear file, mở file nguồn

Nên có drag-and-drop zone và file picker button.

### 3. Panel kiểm tra dữ liệu đầu vào

Mục tiêu là giúp người dùng kiểm tra nhanh dữ liệu trước khi chạy:

- preview vài dòng đầu của từng sheet
- thống kê số dòng hợp lệ / lỗi
- cảnh báo thiếu cột bắt buộc
- cảnh báo sheet không đúng định dạng
- cảnh báo ô trống ở cột quan trọng
- cảnh báo số lượng hoặc thành tiền không parse được

UI nên có badge mức độ:

- OK
- Warning
- Error

### 4. Panel cấu hình xử lý

Cho phép người dùng cấu hình trước khi chạy:

- chọn chế độ match tên hàng
- bật/tắt AI suggestion
- ngưỡng confidence để auto-accept AI match
- rule xử lý đơn vị tính lệch
- rule giá trị mặc định cho hàng không có trong XNT
- lựa chọn có dừng ở bước review hay chạy thẳng

Thiết kế dạng form rõ ràng, có tooltip giải thích từng option.

### 5. Panel tiến trình chạy

Khi xử lý file:

- hiển thị progress bar rõ ràng
- hiển thị từng bước pipeline:
  - đọc file
  - chuẩn hóa dữ liệu
  - match mặt hàng
  - tổng hợp kết quả
  - phát hiện âm kho
  - sinh file output
- hiển thị số lượng record đã xử lý
- hiển thị log realtime ngắn gọn
- có nút hủy hoặc dừng an toàn nếu cần

### 6. Màn hình tổng quan kết quả

Sau khi chạy xong, cần có màn hình summary rõ ràng:

- số mặt hàng trong XNT
- số mặt hàng trên hóa đơn
- số mặt hàng match chính xác
- số mặt hàng match bằng AI
- số mặt hàng cần review
- số cảnh báo lệch đơn vị
- số mặt hàng không có trong XNT
- số trường hợp âm kho
- tổng chênh lệch nhập
- tổng chênh lệch xuất
- tổng chênh lệch tồn cuối

Thiết kế dạng các summary cards + bảng tóm tắt + shortcut action.

### 7. Màn hình review mapping mặt hàng

Đây là màn hình quan trọng nhất. Hãy thiết kế cực kỳ thực dụng và dễ thao tác.

Mỗi dòng nên thể hiện:

- tên hàng trên hóa đơn
- tên hàng gốc XNT được match
- loại match: exact / normalized / AI / not found / need review
- confidence score
- phần số/model/quy cách được highlight
- lý do match hoặc lý do bị nghi ngờ
- đơn vị tính từ hóa đơn
- đơn vị tính từ XNT
- cảnh báo liên quan

Người dùng cần thực hiện được các action:

- confirm match
- đổi sang mặt hàng XNT khác
- đánh dấu là không có trong XNT
- bỏ qua cảnh báo
- tìm kiếm hàng XNT thủ công
- filter theo trạng thái
- bulk approve các case an toàn

Thiết kế nên có:

- bảng chính ở giữa
- panel chi tiết ở bên phải
- search mạnh
- filter chips
- action bar cho thao tác hàng loạt

### 8. Màn hình review cảnh báo đơn vị tính

Màn hình này tập trung vào các case lệch đơn vị:

- đơn vị hóa đơn
- đơn vị gốc XNT
- loại cảnh báo
- mức độ rủi ro
- gợi ý action

Người dùng cần có thể:

- xác nhận vẫn coi là cùng mặt hàng
- giữ cảnh báo nhưng cho phép xuất file
- đưa vào danh sách cần follow-up sau

### 9. Màn hình kết quả chi tiết

Thiết kế bảng dữ liệu lớn, tối ưu cho desktop:

- sticky header
- sortable columns
- resizable columns
- freeze một số cột đầu
- filter theo nhiều điều kiện
- tìm kiếm nhanh
- export current view

Cần hiển thị đầy đủ các cột nghiệp vụ:

- tên hàng chuẩn
- tên XNT
- tên trên hóa đơn
- ĐVT XNT
- ĐVT hóa đơn
- trạng thái match
- cảnh báo
- tồn đầu kỳ số lượng
- tồn đầu kỳ tiền
- mua vào số lượng
- mua vào tiền
- nhập kho số lượng
- nhập kho tiền
- chênh mua nhập số lượng
- chênh mua nhập tiền
- bán ra số lượng
- bán ra tiền
- xuất kho số lượng
- xuất kho tiền
- chênh bán xuất số lượng
- chênh bán xuất tiền
- tồn tính toán số lượng
- tồn tính toán tiền
- tồn cuối kỳ XNT số lượng
- tồn cuối kỳ XNT tiền
- chênh tồn cuối số lượng
- chênh tồn cuối tiền

Nên có hàng tô màu theo mức độ:

- đỏ cho sai lệch lớn
- vàng cho warning
- xanh hoặc trung tính cho khớp tốt

### 10. Màn hình kết quả âm kho

Thiết kế bảng chuyên để điều tra âm kho:

- tên hàng
- ngày phát sinh âm
- tồn đầu kỳ
- lũy kế mua đến ngày
- lũy kế bán đến ngày
- lượng âm thực tế
- tổng giá trị bán trong ngày
- trạng thái match
- cảnh báo

Nên có:

- timeline mini hoặc biểu đồ nhỏ cho từng mặt hàng khi chọn dòng
- panel chi tiết diễn giải vì sao âm kho
- filter theo ngày, mức âm, trạng thái, có/không có trong XNT

### 11. Màn hình export

Cho phép:

- chọn nơi lưu file
- đặt tên file output
- chọn export toàn bộ hoặc chỉ export dữ liệu đã review xong
- chọn có kèm sheet warning/log hay không
- hiển thị preview cấu trúc file sẽ xuất

### 12. Màn hình lịch sử chạy / log

Phải có khả năng xem:

- danh sách các lần chạy
- thời gian chạy
- người dùng thực hiện
- file nguồn đã dùng
- số cảnh báo
- trạng thái thành công/thất bại
- đường dẫn file output

Nên có log detail và khả năng mở lại session review gần nhất.

### 13. Màn hình settings

Gồm các cấu hình hệ thống:

- thư mục output mặc định
- rule normalize tên hàng
- rule cảnh báo đơn vị
- ngưỡng AI confidence
- cấu hình model AI nhỏ nếu dùng
- giới hạn log
- tùy chọn theme sáng/tối nếu cần

## Yêu cầu về trải nghiệm người dùng

1. Phải tối ưu cho màn hình desktop lớn, khoảng `1366px` trở lên.
2. Có thể co giãn hợp lý cho laptop.
3. Bảng dữ liệu là thành phần trung tâm, phải ưu tiên khả năng đọc và thao tác nhanh.
4. Các warning và trạng thái phải nhìn ra ngay bằng màu, icon, badge.
5. Các thao tác review phải giảm số click tối đa.
6. Phải có keyboard-friendly flow cho người dùng làm việc nhiều với bảng.
7. Không dùng phong cách card quá to hoặc spacing kiểu mobile app.
8. Không dùng visual quá màu mè; ưu tiên tin cậy, rõ ràng, doanh nghiệp.

## Yêu cầu về style visual

Thiết kế theo hướng:

- desktop enterprise
- sáng sủa, nghiêm túc, hiện đại
- typography rõ, đậm nhạt tốt
- màu dùng tiết chế
- nhấn mạnh trạng thái xử lý và cảnh báo

Tránh:

- phong cách landing page
- gradient phô trương
- card quá bo tròn
- dashboard web generic
- bố cục thiên về trình diễn hơn thao tác

Có thể dùng phong cách gần với:

- data-heavy admin desktop app
- spreadsheet + inspector panel
- audit/reconciliation workstation

## Yêu cầu kỹ thuật để phù hợp JavaFX / FXML

Thiết kế phải khả thi khi triển khai bằng `JavaFX + FXML`, vì vậy:

- ưu tiên layout rõ ràng bằng `BorderPane`, `SplitPane`, `VBox`, `HBox`, `TabPane`, `TableView`
- tránh những interaction quá phụ thuộc web animation
- tập trung vào component desktop chuẩn:
  - table
  - tree/table
  - form
  - dialog
  - drawer/panel phải
  - wizard stepper nếu hợp lý

## Kết quả mong muốn từ thiết kế

Hãy trả ra đầy đủ:

1. Kiến trúc thông tin của toàn bộ UI.
2. User flow chính từ nhập file đến export.
3. Danh sách màn hình và mục tiêu từng màn hình.
4. Wireframe mô tả chi tiết từng màn hình.
5. Đề xuất layout desktop cụ thể.
6. Danh sách component cần có.
7. Quy tắc màu sắc cho trạng thái:
   - success
   - warning
   - error
   - review needed
   - AI suggested
8. Đề xuất cách hiển thị dữ liệu bảng lớn.
9. Đề xuất các action người dùng cho màn hình review.
10. Một design direction đủ chi tiết để dev có thể dựng lại bằng JavaFX/FXML.

## Yêu cầu đầu ra của AI thiết kế

Nếu trả lời dưới dạng mô tả, hãy trình bày theo thứ tự:

1. Design concept
2. Information architecture
3. Primary user flow
4. Screen-by-screen breakdown
5. Key components
6. Interaction rules
7. Visual system
8. Notes for JavaFX/FXML implementation

Nếu trả lời dưới dạng mockup/wireframe, hãy ưu tiên:

- bố cục rõ ràng
- có đủ các bảng lớn
- có panel review bên phải
- có summary header
- có trạng thái/badge/filter/action rõ ràng

## Yêu cầu bổ sung

UI phải làm nổi bật đây là một tool xử lý nghiệp vụ kiểm tra - đối chiếu - review - xuất báo cáo, không phải chỉ là màn hình upload file đơn giản.

Thiết kế phải thể hiện rõ 3 lớp công việc:

1. `Nạp và kiểm tra dữ liệu`
2. `Đối chiếu và review`
3. `Xem kết quả và xuất báo cáo`

Ưu tiên một trải nghiệm làm việc liên tục, ít chuyển ngữ cảnh, ít popup thừa, nhiều khả năng xử lý trực tiếp ngay trên màn hình chính.

## Prompt rút gọn

Thiết kế UI/UX hoàn chỉnh cho một ứng dụng desktop nội bộ viết bằng Kotlin + JavaFX + FXML để đối chiếu số liệu giữa file XNT, hóa đơn mua vào và hóa đơn bán ra. Ứng dụng cần hỗ trợ nhập file, preview và kiểm tra dữ liệu đầu vào, chạy pipeline đối chiếu, review mapping tên hàng theo exact/normalized/AI, cảnh báo lệch đơn vị tính, xử lý hàng có trên hóa đơn nhưng không có trong XNT, hiển thị kết quả chi tiết so sánh cả số lượng và thành tiền, hiển thị kết quả âm kho theo thời gian, cho phép filter/search/sort/bulk action/export/log/history/settings. Phong cách phải là desktop enterprise, data-heavy, tối ưu cho người dùng nghiệp vụ kế toán/thuế/kho, khả thi để triển khai bằng JavaFX/FXML, không làm kiểu dashboard web generic hoặc landing page.


# Hướng dẫn sử dụng ReconCore

## 1. Mục đích của ứng dụng

`ReconCore` là ứng dụng dùng để:

1. kiểm tra và đối chiếu dữ liệu `XNT`
2. đối chiếu dữ liệu hóa đơn `MuaVao_1`
3. đối chiếu dữ liệu hóa đơn `BanRa_1`
4. review các trường hợp tên hàng khớp chưa chắc chắn
5. review các trường hợp lệch đơn vị tính
6. xem kết quả chi tiết
7. xem các thời điểm âm kho
8. xuất báo cáo kết quả

## 2. Quy trình sử dụng tổng quát

Người dùng sử dụng app theo thứ tự sau:

1. mở ứng dụng
2. kiểm tra nguồn dữ liệu đầu vào
3. kiểm tra validation của file
4. cấu hình rule xử lý nếu cần
5. chạy đối chiếu
6. review các case cần xác nhận
7. xem kết quả chi tiết
8. xem kết quả âm kho
9. xuất file báo cáo

## 3. Các màn hình chính

### 3.1. Workspace

Đây là màn hình tổng quan.

Người dùng có thể xem nhanh:

1. file `XNT`
2. file hóa đơn
3. các sheet đã phát hiện
4. số lượng warning validation
5. số lượng case cần review mapping
6. số lượng case lệch đơn vị
7. số lượng case âm kho

Mục đích:

- kiểm tra nhanh tình trạng dữ liệu trước khi đi sâu vào từng bước

### 3.2. Input / Validation

Màn hình này dùng để kiểm tra dữ liệu đầu vào.

Người dùng có thể:

1. xem trạng thái file `XNT`
2. xem trạng thái file hóa đơn
3. xem sheet `XNT`
4. xem sheet `MuaVao_1`
5. xem sheet `BanRa_1`
6. xem cảnh báo validation

Những gì cần chú ý:

1. thiếu cột bắt buộc
2. dữ liệu ngày không đúng định dạng
3. dữ liệu số lượng hoặc thành tiền bất thường
4. dòng có cảnh báo nghiệp vụ

### 3.3. Configuration

Màn hình này dùng để cấu hình cách app xử lý dữ liệu.

Người dùng có thể chỉnh:

1. cách match tên hàng
2. bật hoặc tắt AI suggestion
3. ngưỡng confidence
4. cách xử lý lệch đơn vị tính
5. cách xử lý hàng không có trong XNT
6. dừng review hay cho chạy tiếp

Lưu ý:

- chỉ thay đổi cấu hình khi thực sự hiểu tác động của rule đó

### 3.4. Execution Progress

Màn hình này hiển thị tiến trình chạy đối chiếu.

Người dùng có thể theo dõi:

1. phần trăm tiến độ
2. bước đang chạy
3. số dòng đã xử lý
4. số warning phát hiện
5. log xử lý

Các bước thường thấy:

1. đọc file
2. kiểm tra cấu trúc
3. phát hiện sheet
4. chuẩn hóa tên hàng
5. match mặt hàng
6. tổng hợp chênh lệch
7. phân tích âm kho
8. chuẩn bị file xuất

### 3.5. Mapping Review

Đây là màn hình dùng để review các trường hợp tên hàng chưa khớp chắc chắn.

Người dùng có thể xem:

1. tên hàng trên hóa đơn
2. tên hàng đang được ghép vào XNT
3. loại match
4. mức confidence
5. đơn vị tính giữa hai bên
6. warning liên quan

Người dùng có thể thao tác:

1. xác nhận match
2. đổi sang mặt hàng XNT khác
3. đánh dấu là không có trong XNT
4. bỏ qua warning

Khi nào cần review kỹ:

1. tên hàng gần giống nhưng khác quy cách số
2. model hoặc kích thước khác nhau
3. đơn vị tính khác nhau
4. hệ thống không tìm thấy trong XNT

### 3.6. Unit Review

Màn hình này dùng để xử lý các case lệch đơn vị tính.

Người dùng có thể xem:

1. đơn vị trên hóa đơn
2. đơn vị trong XNT
3. mức độ nghiêm trọng
4. lý do cảnh báo
5. hướng xử lý đề xuất

Người dùng có thể thao tác:

1. xác nhận vẫn là cùng mặt hàng
2. giữ warning nhưng tiếp tục xử lý
3. nhập quy đổi nếu có
4. đổi lại mặt hàng nếu match sai

### 3.7. Detailed Results

Màn hình này hiển thị kết quả đối chiếu chi tiết.

Người dùng có thể xem:

1. tên hàng chuẩn
2. tên hàng trong XNT
3. tên hàng trên hóa đơn
4. đơn vị tính
5. trạng thái match
6. warning
7. tồn đầu kỳ
8. số lượng và thành tiền mua vào
9. số lượng và thành tiền nhập theo XNT
10. số lượng và thành tiền bán ra
11. số lượng và thành tiền xuất theo XNT
12. tồn tính toán
13. tồn cuối theo XNT
14. chênh lệch cuối kỳ

Mục đích:

- xác định mặt hàng nào đang lệch số liệu

### 3.8. Negative Inventory

Màn hình này dùng để xem các thời điểm âm kho.

Người dùng có thể xem:

1. tên mặt hàng
2. ngày phát sinh âm kho
3. tồn đầu kỳ
4. lũy kế mua
5. lũy kế bán
6. lượng âm thực tế
7. giá trị bán trong ngày
8. trạng thái match
9. warning liên quan

Mục đích:

- xác định nguyên nhân âm kho và các dòng cần kiểm tra lại

### 3.9. Export

Màn hình này dùng để xuất báo cáo kết quả.

Người dùng có thể chọn:

1. nơi lưu file
2. tên file xuất
3. xuất toàn bộ dữ liệu
4. chỉ xuất dữ liệu đã review
5. có kèm warning hoặc log hay không

Trước khi xuất nên kiểm tra:

1. các case review quan trọng đã được xác nhận
2. các case lệch đơn vị đã được xử lý theo mong muốn
3. kết quả âm kho đã được kiểm tra

### 3.10. Run History

Màn hình này dùng để xem lịch sử chạy trước đó.

Người dùng có thể xem:

1. mã lần chạy
2. thời gian chạy
3. trạng thái thành công hoặc chưa hoàn tất
4. log xử lý
5. tóm tắt đầu vào và đầu ra

### 3.11. Settings

Màn hình này dùng để cấu hình chung của ứng dụng.

Người dùng có thể xem hoặc chỉnh:

1. thư mục output mặc định
2. rule normalize
3. rule AI
4. chế độ hiển thị dữ liệu dày hơn hoặc thoáng hơn

## 4. Cách sử dụng khuyến nghị

Để giảm sai sót, nên thao tác theo thứ tự:

1. vào `Workspace` để kiểm tra tổng quan
2. vào `Input / Validation` để kiểm tra dữ liệu đầu vào
3. nếu cần thì chỉnh `Configuration`
4. chạy đối chiếu ở `Execution Progress`
5. review `Mapping Review`
6. review `Unit Review`
7. kiểm tra `Detailed Results`
8. kiểm tra `Negative Inventory`
9. xuất file ở `Export`

## 5. Các điểm người dùng cần chú ý

1. tên hàng giống nhau về chữ nhưng khác quy cách số có thể là mặt hàng khác
2. lệch đơn vị tính cần kiểm tra kỹ trước khi xác nhận
3. hàng có trên hóa đơn nhưng không có trong XNT vẫn có thể xuất hiện trong kết quả
4. màn hình âm kho chỉ hiển thị các thời điểm thực sự bị âm
5. trước khi xuất báo cáo nên review hết các case warning quan trọng

## 6. Khi nào cần kiểm tra lại dữ liệu

Người dùng nên kiểm tra lại khi:

1. số warning tăng bất thường
2. nhiều mặt hàng rơi vào `Not in XNT`
3. xuất hiện nhiều case lệch đơn vị mức cao
4. số lượng mặt hàng âm kho tăng bất thường
5. chênh lệch tồn cuối lớn hơn dự kiến

## 7. Ghi chú hiện tại

Tài liệu này chỉ tập trung vào cách sử dụng app từ góc nhìn người dùng cuối.


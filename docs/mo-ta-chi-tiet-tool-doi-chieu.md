# Mô tả chi tiết tool đối chiếu kê khai thuế và kho

## 1. Mục tiêu

Tool được xây dựng để tự động kiểm tra, so sánh và tổng hợp chênh lệch giữa:

- Sổ `XNT` nội bộ của doanh nghiệp
- Dữ liệu hóa đơn `Mua vào`
- Dữ liệu hóa đơn `Bán ra`

Mục tiêu chính:

1. Đối chiếu số liệu `lượng` và `thành tiền` giữa hóa đơn và XNT.
2. Phát hiện các mặt hàng có chênh lệch nhập/xuất/tồn.
3. Phát hiện các thời điểm phát sinh `âm kho` dựa trên dữ liệu hóa đơn.
4. Cảnh báo các trường hợp sai lệch đơn vị tính hoặc nhận diện mặt hàng không chắc chắn.
5. Vẫn phải trả kết quả cho các mặt hàng có trên hóa đơn nhưng không có trong XNT.

## 2. Bối cảnh nghiệp vụ

Trong bài toán này:

- `XNT` được xem là nguồn dữ liệu gốc về tồn đầu kỳ, nhập, xuất, tồn cuối kỳ và danh mục hàng hóa chuẩn.
- `MuaVao` và `BanRa` là dữ liệu phát sinh thực tế trên hóa đơn dùng để đối chiếu.
- Việc đối chiếu không thể chỉ dựa cứng vào mã hàng vì dữ liệu thực tế có thể không đồng nhất hoàn toàn.
- Trọng tâm nhận diện mặt hàng là `tên hàng`, kèm theo các rule chuẩn hóa và cảnh báo.

## 3. File đầu vào

### 3.1. File `xnt.xlsx`

Vai trò:

- Là dữ liệu chuẩn
- Là nguồn gốc của danh mục hàng hóa
- Là nguồn tham chiếu chính cho `Tên hàng`, `ĐVT`, `Tồn đầu kỳ`, `Nhập`, `Xuất`, `Tồn cuối kỳ`

Thông tin chính lấy từ XNT:

- Tên hàng hóa
- Đơn vị tính
- Mã hàng
- Tồn đầu kỳ: lượng, tiền
- Nhập trong kỳ: lượng, tiền
- Xuất trong kỳ: lượng, tiền
- Tồn cuối kỳ: lượng, tiền

### 3.2. File `chi tiet hoa don anh huy hoang.xlsx`

Gồm 2 sheet:

- `BanRa_1`
- `MuaVao_1`

Vai trò:

- `MuaVao_1`: dữ liệu hóa đơn đầu vào để tổng hợp số lượng mua và thành tiền mua
- `BanRa_1`: dữ liệu hóa đơn đầu ra để tổng hợp số lượng bán và thành tiền bán

Thông tin chính cần dùng:

- Ngày lập hóa đơn
- Tên hàng hóa
- Mã hàng nếu có
- Đơn vị tính
- Số lượng
- Thành tiền

## 4. File đầu ra

Tool cần sinh ra 1 file kết quả theo template logic của `detail.xlsx`, gồm 2 sheet:

1. `KetQua_ChiTiet`
2. `KetQua_AmKho`

Lưu ý:

- Mẫu hiện tại đang thiên về phần `lượng`
- Theo yêu cầu đã chốt, output thực tế phải bổ sung đầy đủ phần `thành tiền`

## 5. Rule nghiệp vụ cốt lõi

### 5.1. XNT là nguồn gốc nhưng không giới hạn phạm vi kết quả

- XNT là danh mục chuẩn và là nguồn chuẩn cho số tồn/nhập/xuất.
- Tuy nhiên, nếu trên `MuaVao` hoặc `BanRa` xuất hiện mặt hàng không có trong XNT thì vẫn phải đưa các mặt hàng này vào kết quả đầu ra.
- Nghĩa là output không được chỉ giới hạn theo danh mục XNT.

### 5.2. Ưu tiên nhận diện theo tên hàng

- Khi đối chiếu giữa XNT và hóa đơn, ưu tiên dùng `tên hàng`.
- Mã hàng chỉ là dữ liệu hỗ trợ nếu có và nếu đủ tin cậy.
- Trong thực tế có thể cần thêm một model AI nhỏ để hỗ trợ nhận diện các tên gần giống nhau.

### 5.3. Rule gộp nhóm và đối chiếu tên hàng

#### Nguyên tắc 1: phần số là dấu hiệu nhận diện mạnh

Các tên/mã có chứa:

- kích thước
- model kèm số
- cân nặng
- thông số kỹ thuật
- quy cách số

thì phải xem là tín hiệu nhận diện mạnh và là `nhóm độc lập`.

Ví dụ tinh thần rule:

- Hai tên hàng có phần chữ giống nhau nhưng khác phần số thì không được gộp tùy tiện.
- `Ống 90`, `Ống 110`, `Ống 140` là các mặt hàng độc lập.
- `Máy A-130`, `Máy A-140` là các mặt hàng độc lập.
- `Keo 5kg`, `Keo 25kg` là các mặt hàng độc lập.

#### Nguyên tắc 2: phần chữ được phép chuẩn hóa mềm

Khi so sánh tên hàng, hệ thống cần cho phép chuẩn hóa các khác biệt kiểu:

- viết hoa / viết thường
- có dấu / không dấu
- có hoặc không có dấu cách
- có hoặc không có dấu chấm
- viết liền / viết tách
- các biến thể chữ mà người dùng đã lưu ý như `i`, `y`, `a`, `â`, `á`, `d`, `z`, `j`, `r` trong bối cảnh tên thực tế bị nhập không thống nhất

Ý nghĩa thực thi:

- Hệ thống cần nới ở phần chữ
- Nhưng vẫn phải giữ chặt phần số

#### Nguyên tắc 3: không gộp sai các mặt hàng chỉ giống phần chữ

Nếu 2 tên hàng:

- giống gần hết phần chữ
- nhưng khác phần số hoặc khác quy cách nhận diện chính

thì phải ưu tiên tách riêng, không auto gộp.

### 5.4. Sai lệch đơn vị tính

Nếu đơn vị tính trên hóa đơn khác với đơn vị tính gốc trên XNT:

- Không chặn xử lý
- Không loại dòng dữ liệu khỏi kết quả
- Chỉ đưa ra `cảnh báo`

Các action cụ thể cho người dùng sẽ được bàn tiếp ở vòng QA sau.

### 5.5. Các trường hợp nhận diện không chắc chắn

Nếu hệ thống không thể match mặt hàng với độ tin cậy cao:

- Vẫn cần đưa dòng vào kết quả
- Gắn trạng thái/cảnh báo phù hợp
- Có thể cần gợi ý match bằng AI hoặc chờ người dùng xác nhận

## 6. Pipeline nhận diện mặt hàng đề xuất

Để hạn chế match sai, quy trình nhận diện nên đi theo nhiều tầng:

### Tầng 1: Match chính xác

- So sánh tên gốc với tên gốc
- So sánh theo key chuẩn hóa đơn giản

### Tầng 2: Match theo normalize

Normalize tên hàng theo các rule:

- lowercase
- trim khoảng trắng thừa
- chuẩn hóa unicode
- bỏ dấu nếu cần
- loại các ký tự ngăn cách như `.`, `-`, khoảng trắng dư
- giữ lại các token số để không làm mất quy cách

### Tầng 3: Match heuristic

Heuristic có thể gồm:

- đối chiếu phần số trước
- sau đó so sánh phần chữ đã normalize
- chỉ chấp nhận khi không có xung đột với mặt hàng khác có phần số khác

### Tầng 4: Gợi ý bằng AI nhỏ

Dùng khi:

- không match được bằng rule cứng
- hoặc có nhiều candidate gần giống nhau

Vai trò của AI:

- gợi ý mặt hàng XNT gần nhất
- đưa ra điểm tin cậy
- không tự động merge vô điều kiện nếu độ tin cậy thấp

### Tầng 5: Cần review

Nếu sau tất cả các bước trên vẫn không chắc chắn:

- đánh dấu `cần review`
- vẫn xuất kết quả
- không được làm mất dữ liệu phát sinh

## 7. Trạng thái nhận diện đề xuất

Mỗi dòng kết quả nên có trạng thái nhận diện để phục vụ UI và QA sau này. Có thể dùng các trạng thái như:

- `MATCH_EXACT`: khớp chính xác
- `MATCH_NORMALIZED`: khớp sau khi chuẩn hóa tên
- `MATCH_AI`: khớp theo gợi ý AI
- `UNIT_WARNING`: khớp mặt hàng nhưng lệch đơn vị
- `NOT_IN_XNT`: có trên hóa đơn nhưng không có trong XNT
- `NEED_REVIEW`: không đủ chắc chắn để kết luận

Một dòng có thể đồng thời có nhiều cảnh báo, ví dụ:

- `MATCH_AI` + `UNIT_WARNING`
- `NOT_IN_XNT` + `NEED_REVIEW`

## 8. Thiết kế sheet `KetQua_ChiTiet`

### 8.1. Mục đích

Sheet này dùng để đối chiếu tổng hợp theo mặt hàng giữa:

- số liệu hóa đơn
- số liệu XNT

### 8.2. Định nghĩa một dòng kết quả

Một dòng trong `KetQua_ChiTiet` đại diện cho một mặt hàng sau khi hệ thống đã gán vào một nhóm nhận diện.

Nhóm này có thể là:

- mặt hàng chuẩn lấy từ XNT
- hoặc mặt hàng phát sinh từ hóa đơn nhưng không có trong XNT

### 8.3. Cột đề xuất

Đề xuất sheet `KetQua_ChiTiet` có tối thiểu các cột sau:

1. `TEN_HANG_HOA_CHUAN`
2. `TEN_GOC_XNT`
3. `TEN_TREN_HD`
4. `DVT_GOC_XNT`
5. `DVT_HD`
6. `TRANG_THAI_MATCH`
7. `CANH_BAO`
8. `TON_DAU_KY_SL_XNT`
9. `TON_DAU_KY_TIEN_XNT`
10. `MUA_VAO_SL_HD`
11. `MUA_VAO_TIEN_HD`
12. `NHAP_KHO_SL_XNT`
13. `NHAP_KHO_TIEN_XNT`
14. `CHENH_MUA_NHAP_SL`
15. `CHENH_MUA_NHAP_TIEN`
16. `BAN_RA_SL_HD`
17. `BAN_RA_TIEN_HD`
18. `XUAT_KHO_SL_XNT`
19. `XUAT_KHO_TIEN_XNT`
20. `CHENH_BAN_XUAT_SL`
21. `CHENH_BAN_XUAT_TIEN`
22. `TON_TINH_TOAN_SL`
23. `TON_TINH_TOAN_TIEN`
24. `TON_CUOI_KY_SL_XNT`
25. `TON_CUOI_KY_TIEN_XNT`
26. `CHENH_TON_CUOI_SL`
27. `CHENH_TON_CUOI_TIEN`
28. `GHI_CHU`

### 8.4. Công thức nghiệp vụ

#### Phần nhập

- `CHENH_MUA_NHAP_SL = MUA_VAO_SL_HD - NHAP_KHO_SL_XNT`
- `CHENH_MUA_NHAP_TIEN = MUA_VAO_TIEN_HD - NHAP_KHO_TIEN_XNT`

#### Phần xuất

- `CHENH_BAN_XUAT_SL = BAN_RA_SL_HD - XUAT_KHO_SL_XNT`
- `CHENH_BAN_XUAT_TIEN = BAN_RA_TIEN_HD - XUAT_KHO_TIEN_XNT`

#### Phần tồn tính toán

- `TON_TINH_TOAN_SL = TON_DAU_KY_SL_XNT + MUA_VAO_SL_HD - BAN_RA_SL_HD`
- `TON_TINH_TOAN_TIEN = TON_DAU_KY_TIEN_XNT + MUA_VAO_TIEN_HD - BAN_RA_TIEN_HD`

#### Phần chênh tồn cuối kỳ

- `CHENH_TON_CUOI_SL = TON_TINH_TOAN_SL - TON_CUOI_KY_SL_XNT`
- `CHENH_TON_CUOI_TIEN = TON_TINH_TOAN_TIEN - TON_CUOI_KY_TIEN_XNT`

### 8.5. Quy tắc fill dữ liệu khi hàng không có trong XNT

Nếu mặt hàng có trên hóa đơn nhưng không có trong XNT:

- Các cột gốc từ XNT để `0` hoặc để trống theo rule kỹ thuật được chốt sau
- `TRANG_THAI_MATCH = NOT_IN_XNT`
- Vẫn phải tổng hợp đầy đủ số mua/bán từ hóa đơn
- Vẫn phải tính chênh theo logic hiện hành

## 9. Thiết kế sheet `KetQua_AmKho`

### 9.1. Mục đích

Sheet này dùng để chỉ ra `thời điểm âm kho`.

Không xuất ra các thời điểm không âm kho.

### 9.2. Nguyên tắc chung

Âm kho được tính theo dòng thời gian phát sinh của hóa đơn:

- Tồn đầu kỳ lấy từ XNT
- Nhập lũy kế lấy từ `MuaVao`
- Xuất lũy kế lấy từ `BanRa`

### 9.3. Công thức nghiệp vụ

Tại một thời điểm kiểm tra:

- `TON_CHAY = TON_DAU_KY_GOC + LUY_KE_MUA_DEN_THOI_DIEM - LUY_KE_BAN_DEN_THOI_DIEM`

Nếu:

- `TON_CHAY < 0`

thì đó là một thời điểm âm kho và phải ghi ra sheet `KetQua_AmKho`.

### 9.4. Ý nghĩa các mốc lũy kế

Theo mô tả đã chốt:

- `Nhập đến thời điểm trước khi âm kho gần nhất`: lấy số lượng nhập lũy kế đến trước thời điểm âm kho
- `Xuất đến thời điểm âm kho`: lấy số lượng xuất lũy kế đến ngày âm kho

### 9.5. Cột đề xuất

1. `TEN_HANG_HOA_CHUAN`
2. `TEN_GOC_XNT`
3. `TEN_TREN_HD`
4. `DVT_GOC_XNT`
5. `DVT_HD`
6. `TRANG_THAI_MATCH`
7. `CANH_BAO`
8. `NGAY_BAN_PHAT_SINH_AM`
9. `TON_DAU_KY_SL_GOC`
10. `LUY_KE_MUA_SL_DEN_NGAY`
11. `LUY_KE_MUA_TIEN_DEN_NGAY`
12. `LUY_KE_BAN_SL_DEN_NGAY`
13. `LUY_KE_BAN_TIEN_DEN_NGAY`
14. `LUONG_AM_KHO_THUC_TE`
15. `TONG_GIA_TRI_BAN_TRONG_NGAY`
16. `GHI_CHU`

### 9.6. Khi nào ghi kết quả

Chỉ ghi kết quả khi:

- mặt hàng bị âm kho

Không ghi:

- các ngày không âm

### 9.7. Trường hợp hàng không có trong XNT

Nếu mặt hàng chỉ có trên hóa đơn và không có trong XNT:

- `TON_DAU_KY_SL_GOC = 0` hoặc trống theo rule kỹ thuật chốt sau
- vẫn tính âm kho theo dữ liệu hóa đơn
- nếu bán ra trước khi có mua vào tương ứng thì có thể phát sinh âm ngay

## 10. Rule sắp xếp dữ liệu để tính âm kho

### 10.1. Bắt buộc sắp xếp theo thời gian

Để tính âm kho, dữ liệu phát sinh phải được sắp xếp theo:

1. ngày chứng từ tăng dần
2. trong cùng ngày, quy tắc ưu tiên xử lý sẽ được chốt sau

### 10.2. Điểm đang mở

Hiện chưa chốt rule:

- Trong cùng một ngày, nếu vừa có `Mua vào` vừa có `Bán ra` thì xử lý thứ tự thế nào

Đây là một open point cần thảo luận tiếp vì nó ảnh hưởng trực tiếp tới việc một ngày có bị âm kho hay không.

## 11. Cảnh báo cần sinh ra

Hệ thống cần có khả năng sinh ít nhất các loại cảnh báo sau:

1. `LECH_DON_VI_TINH`
2. `KHONG_CO_TRONG_XNT`
3. `MATCH_AI_DO_TIN_CAY_THAP`
4. `NHIEU_UNG_VIEN_MATCH`
5. `TEN_HANG_CO_QUY_CACH_SO_KHAC_NHAU`
6. `KHONG_DU_DU_LIEU_DE_KET_LUAN`

Các cảnh báo này cần được đưa ra ở output để phục vụ màn hình action/QA sau này.

## 12. Gợi ý action người dùng cho vòng QA sau

Phần này chưa chốt UI cuối cùng, nhưng về logic nghiệp vụ cần chuẩn bị khả năng cho user:

1. xác nhận một gợi ý match AI
2. đổi mặt hàng XNT được gán
3. bỏ qua cảnh báo đơn vị tính
4. đánh dấu đây là mặt hàng mới ngoài XNT
5. tách một nhóm đang bị gộp sai

## 13. Rủi ro dữ liệu cần lưu ý

### 13.1. Dữ liệu hóa đơn không đồng nhất

- Có thể cùng một mặt hàng nhưng tên viết khác nhau
- Có thể cùng một mặt hàng nhưng đơn vị viết khác nhau
- Có thể có mã hàng ở một số dòng nhưng không có ở dòng khác

### 13.2. Rủi ro gộp sai do fuzzy quá mạnh

Nếu normalize quá mạnh mà bỏ qua phần số:

- rất dễ gộp sai các mặt hàng có cùng tên nền nhưng khác quy cách/model

Do đó:

- phần số phải được xem là tín hiệu nhận diện mạnh

### 13.3. Thành tiền có thể mang tính nhạy cảm nghiệp vụ

Theo requirement hiện tại, `thành tiền` vẫn phải so sánh.

Tuy nhiên khi triển khai thực tế cần lưu ý:

- giá trị trên hóa đơn và giá trị trên XNT có thể khác nhau do cách ghi nhận kế toán hoặc phương pháp tính giá

Hiện tại chưa có rule riêng để điều chỉnh điểm này, nên trước mắt vẫn thực hiện đúng theo yêu cầu đối chiếu số học trực tiếp.

## 14. Logic tổng quát của hệ thống

### Bước 1. Đọc dữ liệu

- Đọc XNT
- Đọc MuaVao
- Đọc BanRa

### Bước 2. Chuẩn hóa dữ liệu đầu vào

- chuẩn hóa tên hàng
- chuẩn hóa đơn vị tính
- chuẩn hóa ngày
- chuẩn hóa số lượng và thành tiền

### Bước 3. Xây bộ danh mục chuẩn

- lấy danh mục chuẩn từ XNT
- tạo key nhận diện cho từng mặt hàng XNT

### Bước 4. Match dữ liệu hóa đơn vào danh mục chuẩn

- exact match
- normalized match
- heuristic match
- AI suggestion
- need review

### Bước 5. Tổng hợp kết quả đối chiếu

- cộng số liệu mua từ MuaVao
- cộng số liệu bán từ BanRa
- lấy số liệu nhập/xuất/tồn từ XNT
- tính chênh lệch

### Bước 6. Tính âm kho

- tạo dòng thời gian phát sinh mua/bán theo từng mặt hàng
- tính tồn chạy
- ghi lại các thời điểm âm kho

### Bước 7. Xuất file kết quả

- sheet `KetQua_ChiTiet`
- sheet `KetQua_AmKho`

## 15. Các điểm đã chốt

1. Output phải thêm cột để phản ánh cả `lượng` và `thành tiền`.
2. Ưu tiên nhận diện mặt hàng theo `tên hàng`.
3. Có thể cần dùng model AI nhỏ để hỗ trợ match tên hàng.
4. Sai lệch đơn vị tính hiện tại chỉ `cảnh báo`.
5. Hàng có trên hóa đơn nhưng không có trong XNT vẫn phải xuất ra ở cả 2 file kết quả.
6. Với tên hàng, phần số/quy cách số phải được coi là tín hiệu nhận diện mạnh và là nhóm độc lập.
7. Chỉ nới lỏng ở phần chữ và hình thức viết tên.

## 16. Các điểm chưa chốt

1. Trong cùng một ngày, nếu vừa có mua vừa có bán thì thứ tự xử lý khi tính âm kho là gì.
2. Khi hàng không có trong XNT, các cột XNT nên để `0` hay để trống ở từng sheet.
3. Ngưỡng tin cậy tối thiểu để AI được phép auto-match là bao nhiêu.
4. Bộ action chính xác cho user trên UI warning/review sẽ được chốt ở vòng QA tiếp theo.


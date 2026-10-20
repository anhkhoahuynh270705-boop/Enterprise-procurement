package com.example.shopping.common.enums;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public enum ErrorEnum {

    // Auth
    INVALID_LOGIN_TRANSACTION       (HttpStatus.BAD_REQUEST,            "Giao dịch đăng nhập không hợp lệ."),
    EXPIRED_LOGIN_TRANSACTION       (HttpStatus.BAD_REQUEST,            "Giao dịch đăng nhập đã hết hạn."),
    INVALID_REFRESH_RESPONSE        (HttpStatus.UNAUTHORIZED,           "Phản hồi làm mới token không hợp lệ."),
    REFRESH_TOKEN_INVALID           (HttpStatus.UNAUTHORIZED,           "Refresh token không hợp lệ hoặc tài khoản không khả dụng."),
    INVALID_TOKEN_ISSUER            (HttpStatus.UNAUTHORIZED,           "Issuer hoặc client của token không hợp lệ."),
    ACCOUNT_UNAVAILABLE             (HttpStatus.UNAUTHORIZED,           "Tài khoản không khả dụng hoặc email chưa xác minh."),
    INVALID_IDENTITY_TOKEN          (HttpStatus.UNAUTHORIZED,           "Identity token không hợp lệ."),

    // Face ID
    FACE_NOT_MATCH                  (HttpStatus.UNAUTHORIZED,           "Khuôn mặt không khớp. Vui lòng thử lại."),
    FACE_NOT_RECOGNIZED             (HttpStatus.UNAUTHORIZED,           "Không nhận diện được khuôn mặt người dùng."),
    FACE_USER_NOT_FOUND             (HttpStatus.UNAUTHORIZED,           "ID người dùng không hợp lệ."),
    FACE_NOT_REGISTERED             (HttpStatus.NOT_FOUND,              "Người dùng chưa đăng ký khuôn mặt."),
    FACE_SESSION_TOKEN_FAILED       (HttpStatus.INTERNAL_SERVER_ERROR,  "Không tạo được token phiên làm việc."),
    FACE_SERVICE_UNAVAILABLE        (HttpStatus.INTERNAL_SERVER_ERROR,  "Dịch vụ xác thực không khả dụng."),
    FACE_SERVICE_ACCOUNT_EMPTY      (HttpStatus.INTERNAL_SERVER_ERROR,  "Service account token trống."),
    FACE_ENROLL_FAILED              (HttpStatus.UNPROCESSABLE_ENTITY,   "Đăng ký khuôn mặt không thành công."),
    FACE_TOKEN_EXCHANGE_FAILED      (HttpStatus.BAD_GATEWAY,             "Không thể cấp token đăng nhập."),

    // User
    USER_NOT_FOUND                  (HttpStatus.NOT_FOUND,              "Không tìm thấy người dùng."),
    USER_PROFILE_NOT_FOUND          (HttpStatus.NOT_FOUND,              "Không tìm thấy hồ sơ người dùng."),
    USER_AVATAR_NOT_FOUND           (HttpStatus.NOT_FOUND,              "Không tìm thấy ảnh đại diện của người dùng."),
    USER_EMPLOYEE_CODE_EXISTS       (HttpStatus.BAD_REQUEST,            "Mã nhân viên đã tồn tại."),
    USER_USERNAME_EXISTS            (HttpStatus.BAD_REQUEST,            "Username đã tồn tại."),
    USER_EMAIL_EXISTS               (HttpStatus.BAD_REQUEST,            "Email đã tồn tại."),
    USER_CANNOT_CHANGE_USERNAME     (HttpStatus.BAD_REQUEST,            "Không được thay đổi username của tài khoản."),
    USER_CANNOT_DISABLE_SELF        (HttpStatus.BAD_REQUEST,            "Bạn không thể tự vô hiệu hóa tài khoản của chính mình."),
    USER_CANNOT_DELETE_SELF         (HttpStatus.BAD_REQUEST,            "Bạn không thể tự xóa tài khoản của chính mình."),
    USER_AVATAR_EMPTY               (HttpStatus.BAD_REQUEST,            "File ảnh không được để trống."),
    USER_AVATAR_INVALID_TYPE        (HttpStatus.BAD_REQUEST,            "Chỉ chấp nhận file hình ảnh."),
    USER_AVATAR_TOO_LARGE           (HttpStatus.BAD_REQUEST,            "Dung lượng ảnh tối đa là 5MB."),
    USER_AVATAR_READ_ERROR          (HttpStatus.INTERNAL_SERVER_ERROR,  "Không thể đọc dữ liệu file ảnh."),
    KEYCLOAK_USER_EXISTS            (HttpStatus.CONFLICT,               "User đã tồn tại trong Keycloak."),
    AUTH_INVALID_BROWSER_RESPONSE   (HttpStatus.BAD_GATEWAY,             "Phản hồi xác thực không hợp lệ."),
    AUTH_BROWSER_HEADER_REQUIRED    (HttpStatus.FORBIDDEN,               "Yêu cầu thiếu thông tin trình duyệt hợp lệ."),
    AUTH_LOGIN_FAILED               (HttpStatus.UNAUTHORIZED,            "Đăng nhập thất bại. Vui lòng thử lại."),
    AUTH_INVALID_TOKEN_RESPONSE     (HttpStatus.BAD_GATEWAY,             "Phản hồi token không hợp lệ."),
    AUTH_CRYPTO_UNAVAILABLE         (HttpStatus.INTERNAL_SERVER_ERROR,   "Không thể khởi tạo bảo mật đăng nhập."),
    AUTH_PENDING_LIMIT_REACHED      (HttpStatus.SERVICE_UNAVAILABLE,      "Hệ thống đang có quá nhiều giao dịch đăng nhập đang chờ."),

    // Supplier
    SUPPLIER_NOT_FOUND              (HttpStatus.NOT_FOUND,              "Không tìm thấy nhà cung cấp."),
    SUPPLIER_TAX_CODE_EXISTS        (HttpStatus.BAD_REQUEST,            "Mã số thuế đã tồn tại."),
    SUPPLIER_NOT_ACTIVE             (HttpStatus.CONFLICT,               "Chỉ được tạo đơn đặt hàng với nhà cung cấp đang hoạt động."),
    SUPPLIER_DOCUMENT_PROCESSED     (HttpStatus.CONFLICT,               "Tài liệu đã được xử lý. Vui lòng tải lại danh sách."),
    SUPPLIER_DOCUMENT_TOO_LARGE     (HttpStatus.PAYLOAD_TOO_LARGE,      "File tối đa 10 MB."),
    SUPPLIER_PROPOSAL_NOT_FOUND     (HttpStatus.NOT_FOUND,              "Không tìm thấy đề xuất nhà cung cấp."),
    SUPPLIER_CONTRACT_NOT_FOUND     (HttpStatus.NOT_FOUND,              "Không tìm thấy hợp đồng nhà cung cấp."),
    SUPPLIER_CONTRACT_NO_ATTACHMENT (HttpStatus.NOT_FOUND,              "Hợp đồng chưa có file đính kèm."),
    SUPPLIER_CONTRACT_INVALID_TITLE (HttpStatus.BAD_REQUEST,            "Tên hợp đồng phải có ít nhất 2 ký tự."),
    SUPPLIER_CONTRACT_EXPIRED_MANUAL(HttpStatus.BAD_REQUEST,            "Trạng thái hết hạn được xác định tự động theo ngày kết thúc."),
    SUPPLIER_CONTRACT_INVALID_DATES (HttpStatus.BAD_REQUEST,            "Ngày kết thúc phải bằng hoặc sau ngày bắt đầu."),
    SUPPLIER_CONTRACT_SIGNED_DATE_INVALID (HttpStatus.BAD_REQUEST,      "Ngày ký không được sau ngày bắt đầu hợp đồng."),
    SUPPLIER_DOCUMENT_NOT_FOUND     (HttpStatus.NOT_FOUND,              "Không tìm thấy tài liệu."),
    SUPPLIER_DOCUMENT_CONTENT_NOT_FOUND (HttpStatus.NOT_FOUND,          "Không tìm thấy nội dung tài liệu."),
    SUPPLIER_DOCUMENT_INVALID       (HttpStatus.BAD_REQUEST,            "File tài liệu nhà cung cấp không hợp lệ."),
    SUPPLIER_DOCUMENT_INVALID_REVIEW(HttpStatus.BAD_REQUEST,             "Thông tin duyệt tài liệu không hợp lệ."),
    SUPPLIER_DOCUMENT_SELF_REVIEW   (HttpStatus.FORBIDDEN,               "Checker không được tự duyệt tài liệu do mình gửi."),
    SUPPLIER_PROPOSAL_PROCESSED     (HttpStatus.CONFLICT,               "Đề xuất này đã được xử lý. Vui lòng tải lại danh sách."),
    SUPPLIER_PROPOSAL_NO_ACTIVE_TASK (HttpStatus.CONFLICT,              "Không tìm thấy nhiệm vụ Camunda đang chờ duyệt cho đề xuất này."),
    SUPPLIER_PROPOSAL_SELF_REVIEW   (HttpStatus.FORBIDDEN,              "Checker không được tự duyệt đề xuất của mình."),
    SUPPLIER_PROPOSAL_INVALID       (HttpStatus.BAD_REQUEST,             "Thông tin đề xuất nhà cung cấp không hợp lệ."),
    SUPPLIER_CONTRACT_NUMBER_FAILED (HttpStatus.INTERNAL_SERVER_ERROR,   "Không nhận được số thứ tự hợp đồng từ cơ sở dữ liệu."),

    // Procurement
    TICKET_NOT_FOUND                (HttpStatus.NOT_FOUND,              "Không tìm thấy phiếu mua sắm."),
    TICKET_DUPLICATE_IMPORT         (HttpStatus.CONFLICT,               "Phiếu mua sắm này trùng với phiếu đã có trong hệ thống."),
    TICKET_NOT_APPROVED             (HttpStatus.CONFLICT,               "Chỉ phiếu mua sắm đã được duyệt mới tạo được đơn đặt hàng."),
    TICKET_ITEM_DUPLICATE           (HttpStatus.BAD_REQUEST,            "Danh sách hạng mục không được chứa dòng trùng lặp."),
    TICKET_ITEM_NOT_IN_TICKET       (HttpStatus.BAD_REQUEST,            "Một hoặc nhiều hạng mục không thuộc phiếu mua sắm đã chọn."),
    TICKET_SELF_APPROVE             (HttpStatus.BAD_REQUEST,            "Không được duyệt phiếu cho chính mình tự tạo."),
    TICKET_NO_PENDING_TASK          (HttpStatus.BAD_REQUEST,            "Phiếu không có nhiệm vụ chờ duyệt hợp lệ."),
    EXCEL_NO_SHEET                  (HttpStatus.BAD_REQUEST,            "File Excel không có sheet nào."),
    EXCEL_INVALID_FILE              (HttpStatus.BAD_REQUEST,            "File Sai định dạng. Vui lòng chọn file Excel .xlsx hoặc .xls không rỗng."),
    EXCEL_UNREADABLE                (HttpStatus.BAD_REQUEST,            "File Sai định dạng. File phải là Excel đọc được, có trang tính và không được khóa bằng mật khẩu."),
    TICKET_CANNOT_SUBMIT            (HttpStatus.CONFLICT,               "Chỉ phiếu ở trạng thái Nháp hoặc Đã từ chối mới được nộp duyệt."),
    TICKET_CANNOT_EDIT              (HttpStatus.CONFLICT,               "Chỉ được sửa phiếu ở trạng thái Nháp hoặc bị từ chối."),
    TICKET_CANNOT_DELETE            (HttpStatus.CONFLICT,               "Chỉ được xóa phiếu ở trạng thái Nháp hoặc bị từ chối."),
    TICKET_UNREGISTERED_SUPPLIERS    (HttpStatus.CONFLICT,               "Không thể duyệt phiếu khi còn nhà cung cấp chưa có trong hệ thống."),
    EXCEL_MISSING_TEMPLATE_HEADERS  (HttpStatus.BAD_REQUEST,            "File Sai định dạng vật tư. Vui lòng dùng đúng mẫu vật tư và thử lại."),
    EXCEL_INVALID_SUPPLIER_TEMPLATE (HttpStatus.BAD_REQUEST,            "File Sai định dạng nhà cung cấp. Vui lòng dùng đúng mẫu nhà cung cấp và thử lại."),

    // Purchase Order
    PURCHASE_ORDER_NOT_FOUND        (HttpStatus.NOT_FOUND,              "Không tìm thấy đơn đặt hàng."),
    PURCHASE_ORDER_ITEM_DUPLICATED  (HttpStatus.CONFLICT,               "Một hoặc nhiều hạng mục đã có trong đơn đặt hàng khác."),
    PURCHASE_ORDER_INVALID_STATUS   (HttpStatus.CONFLICT,               "Không thể chuyển trạng thái đơn hàng hiện tại."),
    PURCHASE_ORDER_NOT_PENDING      (HttpStatus.CONFLICT,               "Đơn đặt hàng này không còn chờ duyệt."),
    PURCHASE_ORDER_SELF_REVIEW      (HttpStatus.FORBIDDEN,              "Checker không được tự duyệt đơn đặt hàng do mình tạo."),
    PURCHASE_ORDER_NO_ACTIVE_TASK   (HttpStatus.CONFLICT,               "Không tìm thấy nhiệm vụ đang chờ duyệt cho đơn này."),

    // Workflow
    CAMUNDA_TASK_NOT_FOUND          (HttpStatus.NOT_FOUND,              "Không tìm thấy nhiệm vụ Camunda."),
    CAMUNDA_TICKET_ID_NOT_FOUND     (HttpStatus.NOT_FOUND,              "Không tìm thấy ticketId cho processInstanceId."),

    // Notification 
    NOTIFICATION_CHANNEL_NOT_FOUND  (HttpStatus.BAD_REQUEST,            "Không tìm thấy NotificationService cho kênh này."),
    COMPANY_DOCUMENT_INVALID        (HttpStatus.BAD_REQUEST,            "Tiêu đề, danh mục hoặc mô tả không hợp lệ."),
    COMPANY_DOCUMENT_NOT_FOUND      (HttpStatus.NOT_FOUND,              "Không tìm thấy tài liệu công ty."),
    ACCESS_DENIED                   (HttpStatus.FORBIDDEN,               "Bạn không có quyền thực hiện thao tác này."),
    TICKET_ACCESS_DENIED            (HttpStatus.FORBIDDEN,               "Bạn không có quyền truy cập phiếu này."),
    JASPER_TEMPLATE_NOT_FOUND       (HttpStatus.INTERNAL_SERVER_ERROR,  "Không tìm thấy template JasperReports."),
    KEYCLOAK_ACCOUNT_NOT_FOUND      (HttpStatus.NOT_FOUND,              "Không tìm thấy tài khoản trên Authentication Server."),
    KEYCLOAK_ROLE_INVALID           (HttpStatus.INTERNAL_SERVER_ERROR,  "Authentication Server trả về role không hợp lệ."),
    KEYCLOAK_UNAVAILABLE            (HttpStatus.SERVICE_UNAVAILABLE,    "Không thể kết nối đến Authentication Server."),
    KEYCLOAK_CREATE_FAILED          (HttpStatus.INTERNAL_SERVER_ERROR,  "Không thể tạo tài khoản trên Authentication Server."),
    KEYCLOAK_PASSWORD_RESET_FAILED  (HttpStatus.INTERNAL_SERVER_ERROR,  "Không thể gửi email đặt lại mật khẩu. Vui lòng thử lại."),
    KEYCLOAK_STATUS_UPDATE_FAILED   (HttpStatus.INTERNAL_SERVER_ERROR,  "Không thể cập nhật trạng thái tài khoản trên Authentication Server."),
    KEYCLOAK_DELETE_FAILED          (HttpStatus.INTERNAL_SERVER_ERROR,  "Không thể xóa tài khoản trên Authentication Server."),

    // Common 
    FILE_TOO_LARGE                  (HttpStatus.PAYLOAD_TOO_LARGE,      "File tối đa 10 MB."),
    DATA_CONFLICT                   (HttpStatus.CONFLICT,               "Dữ liệu bị trùng hoặc không còn hợp lệ. Vui lòng kiểm tra và thử lại."),
    FIELD_NOT_EDITABLE              (HttpStatus.BAD_REQUEST,            "Trường này không được phép cập nhật."),
    DASHBOARD_INVALID_MONTH_FORMAT  (HttpStatus.BAD_REQUEST,            "Tham số month phải có định dạng YYYY-MM.");

    private final HttpStatus status;
    private final String message;

    ErrorEnum(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public ResponseStatusException ex() {
        return new ResponseStatusException(status, message);
    }

    /**
     * Tạo exception với thông báo tùy chỉnh (giữ nguyên HTTP status).
     */
    public ResponseStatusException ex(String customMessage) {
        return new ResponseStatusException(status, customMessage);
    }

    public ResponseStatusException ex(String customMessage, Throwable cause) {
        return new ResponseStatusException(status, customMessage, cause);
    }

    public ResponseStatusException ex(Throwable cause) {
        return new ResponseStatusException(status, message, cause);
    }
}

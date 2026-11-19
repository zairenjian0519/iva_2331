# Historical image cleanup fix

This package is a generic same-profile variant based on the OK3588 package.
The original package and archive are not modified.

The backend now performs the following operations:

- Single and batch alarm deletion remove the database record and its `image/`
  and `ori_image/` files.
- Stored URL values such as `/alarm_picture/image/...`, `/alarmImg/...`, full
  HTTP URLs, and legacy `/home/super/AI_Service/...` paths are mapped to the
  configured alarm root.
- Path traversal and paths outside `image/` or `ori_image/` are rejected.
- Manual clear removes historical alarm images and generated ZIP files while
  preserving `show_image/` camera cover images.
- The alarm root is configured by `KYSCENE_ALARM_ROOT` and defaults to the
  target board path under `/userdata/sps_test/iva-kyscene`.

The Java backend was rebuilt with Maven using the target-board H265 preview
source tree. The package does not install the application on a board during
build; first-board acceptance is still required after manual installation.

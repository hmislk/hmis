# Development Notes

## Adding User Icons

1. Add a new constant to `src/main/java/com/divudi/core/data/Icon.java`. The value should be the label displayed to users.
2. Create an SVG (or gvd) under `src/main/webapp/resources/image/home` with the graphic for the new feature.
3. Update `src/main/webapp/home.xhtml` to reference the new `Icon` constant and image. This home page change should be tracked as a separate issue when creating pull requests.

These guidelines apply to the entire repository.

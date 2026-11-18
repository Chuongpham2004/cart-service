# =====================================================================
# Dockerfile - Cart Service (Tiki-clone)
# File .jar được build ở stage "build" của GitLab CI và truyền sang
# stage "dockerize" qua artifacts => image chỉ chứa JRE + app.jar.
# =====================================================================

# 1. Base image chỉ chứa JRE 17 trên Alpine (gọn nhẹ, ít lỗ hổng)
FROM eclipse-temurin:17-jre-alpine

# 2. Thư mục làm việc trong container
WORKDIR /app

# 3. Tạo user thường, không chạy ứng dụng bằng root
RUN addgroup -S spring && adduser -S spring -G spring

# 4. Copy file jar do Gradle build ra (artifact của stage build)
COPY --chown=spring:spring build/libs/cart-service-1.0.0.jar app.jar

USER spring

# 5. Port của Cart Service
EXPOSE 8080

# 6. Khởi chạy Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]

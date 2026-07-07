FROM node:22-alpine AS build

WORKDIR /app

COPY management-system/fe/package*.json ./
RUN npm ci

COPY management-system/fe/ ./
RUN npm run build

FROM nginx:1.27-alpine

ENV KEYCLOAK_URL=http://localhost:8080 \
    KEYCLOAK_REALM=vdt-shared-lib \
    KEYCLOAK_CLIENT_ID=fe-app \
    KIBANA_DASHBOARD_URL=http://localhost:5601

COPY management-system/fe/nginx.conf /etc/nginx/conf.d/default.conf
COPY management-system/fe/env-config.template.js /usr/share/nginx/html/env-config.template.js
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["/bin/sh", "-c", "envsubst < /usr/share/nginx/html/env-config.template.js > /usr/share/nginx/html/env-config.js && nginx -g 'daemon off;'"]

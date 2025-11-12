# frontend/Dockerfile
FROM node:18 AS build
WORKDIR /app

# copy package files to leverage cache
COPY package*.json ./

# install deps (use install instead of ci to avoid missing lockfile error)
RUN npm install --legacy-peer-deps

# copy rest and build
COPY . .
RUN npm run build --if-present

FROM nginx:stable-alpine
COPY --from=build /app/dist/frontend /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx","-g","daemon off;"]

# Stage 1: Build Angular AppComponent
FROM node:20 AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build --configuration=production

# Stage 2: Serve with Nginx
FROM nginx:alpine
# Remove default nginx html
RUN rm -rf /usr/share/nginx/html/*
# Copy Angular build output (folder name is 'frontend/browser')
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
# Use custom nginx config for Angular SPA routing
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80


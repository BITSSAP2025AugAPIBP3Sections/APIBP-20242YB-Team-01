FROM node:20-alpine
WORKDIR /app
COPY package*.json ./
# install dev deps so TypeScript compiler (tsc) is available for the build step
RUN npm install
COPY . .
RUN npm run build
# runtime port (reads from env PORT, default 8080 in code) - we set 8081 via compose
EXPOSE 8081
CMD ["node", "dist/server.js"]

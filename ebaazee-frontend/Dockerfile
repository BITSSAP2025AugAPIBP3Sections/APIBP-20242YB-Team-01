# Use Node.js LTS version
FROM node:20-alpine

# Set working directory
WORKDIR /app

# Copy package files
COPY package.json package-lock.json* ./

# Install dependencies
RUN npm install

# Copy the rest of the application
COPY . .

# Expose the Vite dev server port
EXPOSE 5173

# Start the development server
# Use --host to make it accessible from outside the container
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0"]

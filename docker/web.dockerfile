FROM node:16 AS build

# Copy the dependency files into the image
WORKDIR /app
COPY package.json package-lock.json ./
# disable ssl verification
RUN npm config set strict-ssl false
RUN npm ci --omit=optional

# Copy the project files into the image
COPY . .
RUN npm i -S @vue/cli-service
RUN npx vue-cli-service build

FROM caddy:2.5.2 AS run

# Copy the build files into the image
COPY --from=build /app/dist /usr/share/caddy


# 前端

bun create nuxt@latest frontend
template -> ui

cd .\frontend\
bun add -d @types/node
bun add zod better-auth
bun run dev

# 后端

cd backend
.\mvnw.cmd spring-boot:run
.\mvnw.cmd -U clean spring-boot:run

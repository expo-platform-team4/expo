# 프론트 이미지. ARM64(Ampere A1) 위에서 돈다.
#
# Node 는 24 로 고정한다. 26 에서는 Next 16 의 rewrites() 가 전부 500 으로 깨지고,
# /api 프록시가 rewrites 라 백엔드 호출이 통째로 실패한다 (AGENTS.md, package.json engines).
# latest 나 lts 를 쓰면 언젠가 26 으로 넘어가면서 조용히 깨진다.
ARG NODE_VERSION=24

# ---------- 의존성 ----------
FROM node:${NODE_VERSION}-slim AS deps

# 이 저장소는 npm 이 아니라 bun 을 쓴다. bun.lock 이 정본이라
# npm install 로 받으면 잠금 파일이 무시되어 버전이 달라질 수 있다.
RUN npm install -g bun

WORKDIR /app
COPY package.json bun.lock ./
RUN bun install --frozen-lockfile

# ---------- 빌드 ----------
FROM node:${NODE_VERSION}-slim AS build
RUN npm install -g bun

WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .

# NEXT_PUBLIC_* 는 런타임이 아니라 빌드 타임에 번들에 박힌다.
# 그래서 이 값만은 이미지를 만들 때 있어야 하고, compose 의 environment 로는 못 바꾼다.
# 비어 있으면 결제창이 뜨지 않는다.
ARG NEXT_PUBLIC_TOSS_CLIENT_KEY
ENV NEXT_PUBLIC_TOSS_CLIENT_KEY=${NEXT_PUBLIC_TOSS_CLIENT_KEY}

# NEXT_PUBLIC_API_BASE_URL 은 굳이 넣지 않는다. 기본값 '/api' 가
# 브라우저에서 same-origin 으로 나가고, Caddy 가 같은 도메인에서 받는다.

RUN bun run build

# ---------- 런타임 ----------
FROM node:${NODE_VERSION}-slim AS runner

ENV NODE_ENV=production
# Next 가 익명 사용 통계를 보내는 것을 끈다.
ENV NEXT_TELEMETRY_DISABLED=1

# node 이미지에 이미 있는 비루트 계정을 쓴다.
USER node
WORKDIR /app

# next.config.ts 의 output: 'standalone' 이 만들어 주는 묶음이다.
# 필요한 node_modules 까지 추려 담겨 있어 이미지가 작다.
# 대신 static 과 public 은 따로 복사해야 한다 — standalone 에 안 들어간다.
COPY --from=build --chown=node:node /app/.next/standalone ./
COPY --from=build --chown=node:node /app/.next/static ./.next/static
COPY --from=build --chown=node:node /app/public ./public

EXPOSE 3000

# standalone 은 자체 server.js 를 낸다. next start 가 아니다.
# 0.0.0.0 으로 바인딩해야 컨테이너 밖(Caddy)에서 붙을 수 있다.
ENV HOSTNAME=0.0.0.0
ENV PORT=3000

CMD ["node", "server.js"]

FROM pgvector/pgvector:0.8.2-pg18

# Install build dependencies + pg dev headers
# (PostgreSQL APT repo is pre-configured in the base image)
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    git \
    curl \
    ca-certificates \
    autoconf \
    automake \
    libtool \
    postgresql-server-dev-18 \
    && rm -rf /var/lib/apt/lists/*

# mecab-ko shared library is in /usr/local/lib — register for runtime linker
RUN echo "/usr/local/lib" > /etc/ld.so.conf.d/mecab.conf

# ── mecab-ko (Korean morphological analyzer) ──────────────────────────────────
WORKDIR /tmp/build

RUN curl -fsSL "https://bitbucket.org/eunjeon/mecab-ko/downloads/mecab-0.996-ko-0.9.2.tar.gz" \
    -o mecab-ko.tar.gz \
    && tar xf mecab-ko.tar.gz \
    && cd mecab-0.996-ko-0.9.2 \
    && case "$(uname -m)" in \
         aarch64) ./configure --build=aarch64-unknown-linux-gnu ;; \
         *)       ./configure ;; \
       esac \
    && make -j"$(nproc)" \
    && make install \
    && ldconfig

# ── mecab-ko-dic (Korean dictionary) ──────────────────────────────────────────
RUN curl -fsSL "https://bitbucket.org/eunjeon/mecab-ko-dic/downloads/mecab-ko-dic-2.1.1-20180720.tar.gz" \
    -o mecab-ko-dic.tar.gz \
    && tar xf mecab-ko-dic.tar.gz \
    && cd mecab-ko-dic-2.1.1-20180720 \
    && autoreconf \
    && case "$(uname -m)" in \
         aarch64) ./configure --build=aarch64-unknown-linux-gnu ;; \
         *)       ./configure ;; \
       esac \
    && make -j"$(nproc)" \
    && make install \
    && ldconfig

# ── textsearch_ko (PostgreSQL FTS extension for Korean) ───────────────────────
RUN git clone --depth 1 https://github.com/i0seph/textsearch_ko.git /tmp/textsearch_ko \
    && cd /tmp/textsearch_ko \
    && PATH="/usr/lib/postgresql/18/bin:$PATH" make USE_PGXS=1 install

# Cleanup
RUN rm -rf /tmp/build /tmp/textsearch_ko

WORKDIR /

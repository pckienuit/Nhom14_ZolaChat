#!/bin/bash

# Janus Gateway Installation Script for Ubuntu
# VPS: 163.61.182.20
# Domain: zolachat.site

set -e

echo "========================================="
echo " Janus Gateway Installation for ZolaChat"
echo "========================================="

# Update system
echo "[1/7] Updating system packages..."
apt update && apt upgrade -y

# Install dependencies
echo "[2/7] Installing dependencies..."
apt install -y \
    libmicrohttpd-dev libjansson-dev libssl-dev \
    libsofia-sip-ua-dev libglib2.0-dev libopus-dev \
    libogg-dev libcurl4-openssl-dev liblua5.3-dev \
    libconfig-dev pkg-config gengetopt libtool \
    automake cmake git nginx certbot python3-certbot-nginx \
    build-essential python3-pip

# Install meson and ninja for building libnice
echo "[2.5/7] Installing meson and ninja..."
pip3 install meson ninja

# Install libnice (required for WebRTC)
echo "[3/7] Installing libnice..."
cd /tmp
rm -rf libnice  # Clean up if exists from previous run
git clone https://gitlab.freedesktop.org/libnice/libnice.git
cd libnice
meson setup build
ninja -C build
ninja -C build install
ldconfig

# Install libsrtp (required for media encryption)
echo "[4/7] Installing libsrtp..."
cd /tmp
rm -rf libsrtp-2.5.0 v2.5.0.tar.gz  # Clean up if exists
wget https://github.com/cisco/libsrtp/archive/v2.5.0.tar.gz
tar xvf v2.5.0.tar.gz
cd libsrtp-2.5.0
./configure --prefix=/usr --enable-openssl
make shared_library
make install
ldconfig

# Install libwebsockets (required for WebSocket transport)
echo "[4.5/7] Installing libwebsockets..."
cd /tmp
rm -rf libwebsockets v4.3.3.tar.gz  # Clean up if exists
wget https://github.com/warmcat/libwebsockets/archive/v4.3.3.tar.gz
tar xvf v4.3.3.tar.gz
cd libwebsockets-4.3.3
mkdir build
cd build
cmake -DCMAKE_INSTALL_PREFIX:PATH=/usr -DLWS_MAX_SMP=1 -DLWS_WITHOUT_EXTENSIONS=0 ..
make
make install
ldconfig

# Clone and build Janus
echo "[5/7] Cloning and building Janus Gateway..."
cd /opt
rm -rf janus-gateway  # Clean up if exists from previous run
git clone https://github.com/meetecho/janus-gateway.git
cd janus-gateway
sh autogen.sh

# Configure with WebSocket support
./configure \
    --prefix=/opt/janus \
    --enable-websockets \
    --disable-post-processing

make
make install
make configs

# Create systemd service
echo "[6/7] Creating systemd service..."
cat > /etc/systemd/system/janus.service <<EOF
[Unit]
Description=Janus WebRTC Server
After=network.target

[Service]
Type=simple
ExecStart=/opt/janus/bin/janus -F /opt/janus/etc/janus
Restart=always
RestartSec=3

[Install]
WantedBy=multi-user.target
EOF

# Configure firewall
echo "[7/7] Configuring firewall..."
ufw allow 22/tcp     # SSH
ufw allow 80/tcp     # HTTP
ufw allow 443/tcp    # HTTPS
ufw allow 8088/tcp   # Janus HTTP
ufw allow 8089/tcp   # Janus HTTPS
ufw allow 8188/tcp   # Janus WebSocket
ufw allow 8989/tcp   # Janus WebSocket Secure
ufw allow 10000:10200/udp  # RTP/RTCP for media
ufw --force enable

# Reload systemd and start Janus
systemctl daemon-reload
systemctl enable janus
systemctl start janus

echo ""
echo "========================================="
echo " Installation Complete!"
echo "========================================="
echo ""
echo "Next steps:"
echo "1. Configure Janus: /opt/janus/etc/janus/"
echo "2. Setup SSL: Run ssl-setup.sh"
echo "3. Check status: systemctl status janus"
echo ""
echo "Janus is running on:"
echo "  - HTTP:      http://163.61.182.20:8088"
echo "  - WebSocket: ws://163.61.182.20:8188"
echo ""

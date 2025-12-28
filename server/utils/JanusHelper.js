// Janus Gateway Helper for ZolaChat Video Calls
// Manages Janus rooms and sessions

const axios = require('axios');
const WebSocket = require('ws');

class JanusHelper {
    constructor(config = {}) {
        this.httpUrl = config.httpUrl || 'http://localhost:8088/janus';
        this.wsUrl = config.wsUrl || 'ws://localhost:8188';
        this.apiSecret = config.apiSecret || 'ZolaChat2025SecretKey';
        this.adminSecret = config.adminSecret || 'ZolaChat2025AdminKey';
        
        this.sessions = new Map(); // sessionId -> { participantId, handleId, roomId }
    }

    /**
     * Create a new Janus session
     */
    async createSession() {
        try {
            const response = await axios.post(this.httpUrl, {
                janus: 'create',
                transaction: this._generateTransactionId(),
                apisecret: this.apiSecret
            });

            const sessionId = response.data.data.id;
            console.log(`[Janus] Session created: ${sessionId}`);
            return sessionId;
        } catch (error) {
            console.error('[Janus] Failed to create session:', error.message);
            throw error;
        }
    }

    /**
     * Attach to VideoRoom plugin
     */
    async attachToVideoRoom(sessionId) {
        try {
            const response = await axios.post(`${this.httpUrl}/${sessionId}`, {
                janus: 'attach',
                plugin: 'janus.plugin.videoroom',
                transaction: this._generateTransactionId(),
                apisecret: this.apiSecret
            });

            const handleId = response.data.data.id;
            console.log(`[Janus] Attached to VideoRoom: ${handleId}`);
            return handleId;
        } catch (error) {
            console.error('[Janus] Failed to attach to VideoRoom:', error.message);
            throw error;
        }
    }

    /**
     * Create a VideoRoom (for a call)
     * @param {string} callId - Unique call ID from Firestore
     */
    async createRoom(callId, options = {}) {
        try {
            const sessionId = await this.createSession();
            const handleId = await this.attachToVideoRoom(sessionId);

            const request = {
                janus: 'message',
                transaction: this._generateTransactionId(),
                body: {
                    request: 'create',
                    room: callId,
                    permanent: false, // Room auto-destroyed when empty
                    description: `ZolaChat Call ${callId}`,
                    publishers: 2, // Max 2 participants for 1-on-1
                    bitrate: 256000, // 256kbps
                    fir_freq: 10,
                    audiocodec: 'opus',
                    videocodec: 'vp8',
                    admin_key: this.adminSecret,
                    ...options
                },
                apisecret: this.apiSecret
            };

            const response = await axios.post(`${this.httpUrl}/${sessionId}/${handleId}`, request);
            
            console.log(`[Janus] Room created: ${callId}`);
            return {
                roomId: callId,
                sessionId,
                handleId,
                success: true
            };
        } catch (error) {
            console.error('[Janus] Failed to create room:', error.message);
            throw error;
        }
    }

    /**
     * Destroy a room when call ends
     */
    async destroyRoom(callId, sessionId, handleId) {
        try {
            const request = {
                janus: 'message',
                transaction: this._generateTransactionId(),
                body: {
                    request: 'destroy',
                    room: callId,
                    admin_key: this.adminSecret
                },
                apisecret: this.apiSecret
            };

            await axios.post(`${this.httpUrl}/${sessionId}/${handleId}`, request);
            console.log(`[Janus] Room destroyed: ${callId}`);
        } catch (error) {
            console.error('[Janus] Failed to destroy room:', error.message);
        }
    }

    /**
     * Get room info
     */
    async getRoomInfo(callId) {
        try {
            const sessionId = await this.createSession();
            const handleId = await this.attachToVideoRoom(sessionId);

            const request = {
                janus: 'message',
                transaction: this._generateTransactionId(),
                body: {
                    request: 'exists',
                    room: callId
                },
                apisecret: this.apiSecret
            };

            const response = await axios.post(`${this.httpUrl}/${sessionId}/${handleId}`, request);
            return response.data.plugindata.data;
        } catch (error) {
            console.error('[Janus] Failed to get room info:', error.message);
            return { exists: false };
        }
    }

    /**
     * List participants in a room
     */
    async listParticipants(callId, sessionId, handleId) {
        try {
            const request = {
                janus: 'message',
                transaction: this._generateTransactionId(),
                body: {
                    request: 'listparticipants',
                    room: callId
                },
                apisecret: this.apiSecret
            };

            const response = await axios.post(`${this.httpUrl}/${sessionId}/${handleId}`, request);
            return response.data.plugindata.data.participants || [];
        } catch (error) {
            console.error('[Janus] Failed to list participants:', error.message);
            return [];
        }
    }

    /**
     * Generate random transaction ID
     */
    _generateTransactionId() {
        return Math.random().toString(36).substring(2, 15);
    }
}

module.exports = JanusHelper;

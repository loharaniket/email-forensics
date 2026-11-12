(function () {
  'use strict';

  if (window.__EMAIL_FORENSICS_LOADED__) return;
  window.__EMAIL_FORENSICS_LOADED__ = true;

  function escapeHtml(str) {
    if (str === null || str === undefined) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  class EmailButton {
    constructor(onClick) {
      this.onClick = onClick;
      this.host = null;
      this.shadow = null;
      this.button = null;
      this.label = null;
      this.icon = null;
      this.isLoading = false;
    }

    isInjected() {
      return Boolean(document.getElementById('email-forensics-btn-host'));
    }

    inject(toolbarElement) {
      if (!toolbarElement || this.isInjected()) return false;

      const host = document.createElement('div');
      host.id = 'email-forensics-btn-host';
      host.style.display = 'inline-flex';
      host.style.alignItems = 'center';
      host.style.verticalAlign = 'middle';
      host.style.margin = '0 6px';

      const shadow = host.attachShadow({ mode: 'open' });
      const style = document.createElement('style');
      style.textContent = `
        * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; }
        .forensics-btn {
          display: inline-flex; align-items: center; gap: 6px; padding: 0 12px; height: 32px;
          background: #2563EB; color: #FFFFFF; border: 1px solid #1D4ED8; border-radius: 16px;
          font-size: 12px; font-weight: 600; cursor: pointer; transition: all 0.15s ease;
          box-shadow: 0 1px 2px rgba(0, 0, 0, 0.08); user-select: none;
        }
        .forensics-btn:hover:not(:disabled) { background: #1D4ED8; }
        .forensics-btn:disabled { opacity: 0.65; cursor: not-allowed; }
        .btn-icon { width: 14px; height: 14px; fill: none; stroke: currentColor; stroke-width: 2.2; flex-shrink: 0; }
        .spinner { animation: spin 0.9s linear infinite; }
        @keyframes spin { 100% { transform: rotate(360deg); } }
      `;

      const btn = document.createElement('button');
      btn.className = 'forensics-btn';
      btn.setAttribute('type', 'button');
      btn.innerHTML = `
        <svg class="btn-icon" viewBox="0 0 24 24"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/><polyline points="9 12 11 14 15 10"/></svg>
        <span class="btn-label">Analyze Email</span>
      `;

      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        e.preventDefault();
        if (!this.isLoading && typeof this.onClick === 'function') {
          this.onClick();
        }
      });

      shadow.appendChild(style);
      shadow.appendChild(btn);

      this.host = host;
      this.shadow = shadow;
      this.button = btn;
      this.label = btn.querySelector('.btn-label');
      this.icon = btn.querySelector('.btn-icon');

      toolbarElement.appendChild(host);
      return true;
    }

    setLoading(loading, text = 'Analyzing...') {
      this.isLoading = Boolean(loading);
      if (!this.button) return;
      this.button.disabled = this.isLoading;
      if (this.isLoading) {
        if (this.label) this.label.textContent = text;
        if (this.icon) {
          this.icon.classList.add('spinner');
          this.icon.innerHTML = `<circle cx="12" cy="12" r="10" stroke="currentColor" stroke-dasharray="32" stroke-linecap="round"/>`;
        }
      } else {
        if (this.label) this.label.textContent = 'Analyze Email';
        if (this.icon) {
          this.icon.classList.remove('spinner');
          this.icon.innerHTML = `<path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/><polyline points="9 12 11 14 15 10"/>`;
        }
      }
    }

    remove() {
      if (this.host && this.host.parentNode) {
        this.host.parentNode.removeChild(this.host);
      }
      this.host = null;
      this.shadow = null;
      this.button = null;
      this.label = null;
      this.icon = null;
      this.isLoading = false;
    }
  }

  class MessageSelector {
    constructor(onSelectMessage) {
      this.onSelectMessage = onSelectMessage;
      this.shadowHost = null;
      this.shadowRoot = null;
      this.selectedMessageId = null;
    }

    show(messages, suggestedId = null) {
      this.close();

      if (!messages || messages.length === 0) {
        return;
      }

      this.selectedMessageId = suggestedId || messages[messages.length - 1].id;

      const host = document.createElement('div');
      host.id = 'email-forensics-selector-host';
      host.style.position = 'fixed';
      host.style.top = '0';
      host.style.left = '0';
      host.style.width = '100vw';
      host.style.height = '100vh';
      host.style.zIndex = '999998';
      host.style.display = 'flex';
      host.style.alignItems = 'center';
      host.style.justifyContent = 'center';
      host.style.backgroundColor = 'rgba(15, 23, 42, 0.45)';
      host.style.backdropFilter = 'blur(3px)';

      const shadow = host.attachShadow({ mode: 'open' });

      const style = document.createElement('style');
      style.textContent = `
        * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; }
        .modal-card {
          width: 520px; max-width: 90vw; max-height: 80vh; background: #ffffff; border-radius: 12px;
          box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
          display: flex; flex-direction: column; overflow: hidden; border: 1px solid #E2E8F0;
          animation: popIn 0.18s cubic-bezier(0.16, 1, 0.3, 1);
        }
        @keyframes popIn { from { opacity: 0; transform: scale(0.96); } to { opacity: 1; transform: scale(1); } }
        .modal-header {
          padding: 16px 20px; border-bottom: 1px solid #E2E8F0; display: flex; align-items: center;
          justify-content: space-between; background-color: #F8FAFC;
        }
        .header-title-group { display: flex; align-items: center; gap: 10px; }
        .header-icon {
          width: 28px; height: 28px; border-radius: 6px; background: #EFF6FF; color: #2563EB;
          display: flex; align-items: center; justify-content: center;
        }
        .header-icon svg { width: 16px; height: 16px; fill: none; stroke: currentColor; stroke-width: 2; }
        .header-title { font-size: 15px; font-weight: 600; color: #0F172A; }
        .header-close {
          background: none; border: none; font-size: 20px; line-height: 1; color: #64748B;
          cursor: pointer; padding: 4px 8px; border-radius: 4px;
        }
        .header-close:hover { color: #0F172A; background-color: #E2E8F0; }
        .modal-body {
          padding: 16px 20px; overflow-y: auto; flex: 1; display: flex; flex-direction: column; gap: 10px;
        }
        .body-desc { font-size: 13px; color: #64748B; margin-bottom: 6px; }
        .message-item {
          display: flex; align-items: flex-start; gap: 12px; padding: 12px 14px; border: 1px solid #E2E8F0;
          border-radius: 8px; cursor: pointer; transition: all 0.15s ease; background: #FFFFFF;
        }
        .message-item:hover { border-color: #93C5FD; background-color: #F8FAFC; }
        .message-item.selected { border-color: #2563EB; background-color: #EFF6FF; }
        .radio-btn { margin-top: 3px; accent-color: #2563EB; cursor: pointer; }
        .message-details { flex: 1; overflow: hidden; }
        .message-sender-row { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin-bottom: 2px; }
        .message-sender { font-size: 13px; font-weight: 600; color: #1E293B; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
        .message-date { font-size: 11px; color: #64748B; white-space: nowrap; }
        .message-subject { font-size: 12px; font-weight: 500; color: #334155; margin-bottom: 4px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
        .message-snippet {
          font-size: 12px; color: #64748B; display: -webkit-box; -webkit-line-clamp: 2;
          -webkit-box-orient: vertical; overflow: hidden; line-height: 1.4;
        }
        .modal-footer {
          padding: 14px 20px; border-top: 1px solid #E2E8F0; display: flex; align-items: center;
          justify-content: flex-end; gap: 10px; background-color: #F8FAFC;
        }
        .btn-secondary {
          padding: 8px 14px; font-size: 13px; font-weight: 500; color: #475569; background: #FFFFFF;
          border: 1px solid #CBD5E1; border-radius: 6px; cursor: pointer;
        }
        .btn-secondary:hover { background-color: #F1F5F9; }
        .btn-primary {
          padding: 8px 16px; font-size: 13px; font-weight: 500; color: #FFFFFF; background: #2563EB;
          border: 1px solid #1D4ED8; border-radius: 6px; cursor: pointer;
        }
        .btn-primary:hover { background-color: #1D4ED8; }
      `;

      const card = document.createElement('div');
      card.className = 'modal-card';

      const header = document.createElement('div');
      header.className = 'modal-header';
      header.innerHTML = `
        <div class="header-title-group">
          <div class="header-icon">
            <svg viewBox="0 0 24 24"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg>
          </div>
          <span class="header-title">Select Message to Analyze</span>
        </div>
        <button class="header-close" title="Close">&times;</button>
      `;

      const body = document.createElement('div');
      body.className = 'modal-body';

      const desc = document.createElement('p');
      desc.className = 'body-desc';
      desc.textContent = `This conversation contains ${messages.length} messages. Select the specific email to inspect:`;
      body.appendChild(desc);

      const itemsContainer = document.createElement('div');
      itemsContainer.style.display = 'flex';
      itemsContainer.style.flexDirection = 'column';
      itemsContainer.style.gap = '8px';

      messages.forEach((msg, idx) => {
        const item = document.createElement('div');
        item.className = 'message-item' + (msg.id === this.selectedMessageId ? ' selected' : '');
        item.dataset.id = msg.id;

        const radio = document.createElement('input');
        radio.type = 'radio';
        radio.name = 'forensics-msg-choice';
        radio.className = 'radio-btn';
        radio.checked = msg.id === this.selectedMessageId;

        const details = document.createElement('div');
        details.className = 'message-details';

        const row = document.createElement('div');
        row.className = 'message-sender-row';

        const sender = document.createElement('span');
        sender.className = 'message-sender';
        sender.textContent = msg.from || `Message #${idx + 1}`;

        const date = document.createElement('span');
        date.className = 'message-date';
        date.textContent = msg.date || '';

        row.appendChild(sender);
        row.appendChild(date);

        const subj = document.createElement('div');
        subj.className = 'message-subject';
        subj.textContent = msg.subject || '(No subject)';

        const snip = document.createElement('div');
        snip.className = 'message-snippet';
        snip.textContent = msg.snippet || '';

        details.appendChild(row);
        details.appendChild(subj);
        details.appendChild(snip);

        item.appendChild(radio);
        item.appendChild(details);

        item.addEventListener('click', () => {
          this.selectedMessageId = msg.id;
          itemsContainer.querySelectorAll('.message-item').forEach((el) => {
            el.classList.remove('selected');
            const r = el.querySelector('input[type="radio"]');
            if (r) r.checked = false;
          });
          item.classList.add('selected');
          radio.checked = true;
        });

        itemsContainer.appendChild(item);
      });

      body.appendChild(itemsContainer);

      const footer = document.createElement('div');
      footer.className = 'modal-footer';

      const cancelBtn = document.createElement('button');
      cancelBtn.className = 'btn-secondary';
      cancelBtn.textContent = 'Cancel';
      cancelBtn.addEventListener('click', () => this.close());

      const confirmBtn = document.createElement('button');
      confirmBtn.className = 'btn-primary';
      confirmBtn.textContent = 'Analyze Selected';
      confirmBtn.addEventListener('click', () => {
        const chosenId = this.selectedMessageId;
        this.close();
        if (chosenId && typeof this.onSelectMessage === 'function') {
          const chosenMsg = messages.find((m) => m.id === chosenId);
          this.onSelectMessage(chosenId, chosenMsg);
        }
      });

      footer.appendChild(cancelBtn);
      footer.appendChild(confirmBtn);

      card.appendChild(header);
      card.appendChild(body);
      card.appendChild(footer);

      shadow.appendChild(style);
      shadow.appendChild(card);

      host.addEventListener('click', (e) => {
        if (e.target === host) {
          this.close();
        }
      });

      header.querySelector('.header-close').addEventListener('click', () => this.close());

      this.shadowHost = host;
      this.shadowRoot = shadow;
      document.body.appendChild(host);
    }

    close() {
      if (this.shadowHost && this.shadowHost.parentNode) {
        this.shadowHost.parentNode.removeChild(this.shadowHost);
      }
      this.shadowHost = null;
      this.shadowRoot = null;
    }
  }

  class ResultPanel {
    constructor() {
      this.shadowHost = null;
      this.shadowRoot = null;
    }

    ensureHost() {
      if (this.shadowHost && document.body.contains(this.shadowHost)) {
        return this.shadowHost;
      }

      const host = document.createElement('div');
      host.id = 'email-forensics-panel-host';
      host.style.position = 'fixed';
      host.style.top = '0';
      host.style.right = '0';
      host.style.width = '420px';
      host.style.height = '100vh';
      host.style.zIndex = '999999';
      host.style.pointerEvents = 'none';

      const shadow = host.attachShadow({ mode: 'open' });
      this.shadowHost = host;
      this.shadowRoot = shadow;
      document.body.appendChild(host);

      return host;
    }

    applyStyles() {
      if (this.shadowRoot.querySelector('style')) return;

      const style = document.createElement('style');
      style.textContent = `
        * {
          box-sizing: border-box;
          margin: 0;
          padding: 0;
          font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
        }

        .panel-container {
          pointer-events: auto;
          width: 420px;
          height: 100vh;
          background: #FFFFFF;
          border-left: 1px solid #E2E8F0;
          box-shadow: -4px 0 24px rgba(0, 0, 0, 0.12);
          display: flex;
          flex-direction: column;
          position: absolute;
          top: 0;
          right: 0;
          transform: translateX(100%);
          transition: transform 0.24s cubic-bezier(0.16, 1, 0.3, 1);
          overflow: hidden;
          color: #0F172A;
        }

        .panel-container.open {
          transform: translateX(0);
        }

        .panel-header {
          padding: 16px 20px;
          border-bottom: 1px solid #E2E8F0;
          display: flex;
          align-items: center;
          justify-content: space-between;
          background: #F8FAFC;
          flex-shrink: 0;
        }

        .brand-group {
          display: flex;
          align-items: center;
          gap: 10px;
        }

        .brand-icon {
          width: 32px;
          height: 32px;
          border-radius: 8px;
          background: #2563EB;
          color: #FFFFFF;
          display: flex;
          align-items: center;
          justify-content: center;
          box-shadow: 0 2px 4px rgba(37, 99, 235, 0.25);
        }

        .brand-icon svg {
          width: 18px;
          height: 18px;
          fill: none;
          stroke: currentColor;
          stroke-width: 2.2;
        }

        .brand-text h2 {
          font-size: 15px;
          font-weight: 700;
          color: #0F172A;
        }

        .brand-text span {
          font-size: 11px;
          color: #64748B;
          display: block;
        }

        .close-btn {
          width: 32px;
          height: 32px;
          border-radius: 6px;
          border: none;
          background: transparent;
          color: #64748B;
          font-size: 22px;
          line-height: 1;
          display: flex;
          align-items: center;
          justify-content: center;
          cursor: pointer;
        }

        .close-btn:hover {
          background: #E2E8F0;
          color: #0F172A;
        }

        .panel-body {
          flex: 1;
          overflow-y: auto;
          padding: 18px 20px;
          display: flex;
          flex-direction: column;
          gap: 16px;
          background: #FFFFFF;
        }

        .context-card {
          background: #F8FAFC;
          border: 1px solid #E2E8F0;
          border-radius: 10px;
          padding: 12px 14px;
          display: flex;
          flex-direction: column;
          gap: 6px;
        }

        .context-subject {
          font-size: 14px;
          font-weight: 600;
          color: #0F172A;
          line-height: 1.35;
          word-break: break-word;
        }

        .context-meta {
          font-size: 12px;
          color: #64748B;
          display: flex;
          flex-direction: column;
          gap: 3px;
        }

        .meta-line {
          display: flex;
          align-items: baseline;
          gap: 6px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .meta-label {
          font-weight: 600;
          color: #475569;
          font-size: 11px;
          min-width: 44px;
        }

        .meta-val {
          color: #1E293B;
          overflow: hidden;
          text-overflow: ellipsis;
        }

        .progress-card {
          background: #EFF6FF;
          border: 1px solid #BFDBFE;
          border-radius: 10px;
          padding: 16px;
          display: flex;
          flex-direction: column;
          gap: 10px;
        }

        .progress-header {
          display: flex;
          align-items: center;
          gap: 8px;
          color: #1D4ED8;
          font-size: 13px;
          font-weight: 600;
        }

        .progress-spinner {
          width: 16px;
          height: 16px;
          border: 2px solid #93C5FD;
          border-top-color: #1D4ED8;
          border-radius: 50%;
          animation: spin 0.8s linear infinite;
        }

        .progress-bar-bg {
          width: 100%;
          height: 6px;
          background: #DBEAFE;
          border-radius: 4px;
          overflow: hidden;
        }

        .progress-bar-fill {
          height: 100%;
          background: #2563EB;
          border-radius: 4px;
          transition: width 0.3s ease;
        }

        .progress-desc {
          font-size: 11.5px;
          color: #3B82F6;
        }

        .risk-card {
          background: #FFFFFF;
          border: 1px solid #E2E8F0;
          border-radius: 10px;
          padding: 16px;
          display: flex;
          flex-direction: column;
          gap: 12px;
        }

        .risk-top-row {
          display: flex;
          align-items: center;
          justify-content: space-between;
        }

        .score-box {
          display: flex;
          align-items: baseline;
          gap: 3px;
        }

        .score-num {
          font-size: 32px;
          font-weight: 800;
          color: #0F172A;
          line-height: 1;
        }

        .score-max {
          font-size: 14px;
          font-weight: 600;
          color: #94A3B8;
        }

        .score-label {
          font-size: 11px;
          color: #64748B;
          margin-top: 2px;
        }

        .badge {
          display: inline-flex;
          align-items: center;
          padding: 4px 10px;
          border-radius: 20px;
          font-size: 11px;
          font-weight: 700;
          text-transform: uppercase;
        }

        .badge-low { background: #DCFCE7; color: #166534; }
        .badge-moderate { background: #FEF9C3; color: #854D0E; }
        .badge-high { background: #FFEDD5; color: #9A3412; }
        .badge-critical { background: #FEE2E2; color: #991B1B; }

        .confidence-row {
          display: flex;
          justify-content: space-between;
          font-size: 11.5px;
          color: #64748B;
          padding-top: 8px;
          border-top: 1px solid #F1F5F9;
        }

        .ai-card {
          background: #FFFFFF;
          border: 1px solid #E2E8F0;
          border-radius: 10px;
          padding: 16px;
          display: flex;
          flex-direction: column;
          gap: 10px;
        }

        .ai-header {
          display: flex;
          align-items: center;
          justify-content: space-between;
        }

        .ai-title {
          font-size: 13px;
          font-weight: 700;
          color: #1E293B;
          display: flex;
          align-items: center;
          gap: 6px;
        }

        .ai-title svg {
          width: 15px;
          height: 15px;
          color: #2563EB;
          fill: none;
          stroke: currentColor;
          stroke-width: 2;
        }

        .ai-summary {
          font-size: 12.5px;
          line-height: 1.5;
          color: #1E293B;
          background: #F8FAFC;
          border-radius: 8px;
          padding: 10px 12px;
          border: 1px solid #E2E8F0;
        }

        .ai-indicators-list {
          list-style: none;
          display: flex;
          flex-direction: column;
          gap: 4px;
        }

        .ai-indicator-item {
          display: flex;
          align-items: flex-start;
          gap: 6px;
          font-size: 11.5px;
          color: #334155;
          line-height: 1.35;
        }

        .ai-indicator-item::before {
          content: "•";
          color: #EF4444;
          font-size: 14px;
          font-weight: bold;
        }

        .findings-section {
          display: flex;
          flex-direction: column;
          gap: 8px;
        }

        .section-label {
          font-size: 11px;
          text-transform: uppercase;
          font-weight: 700;
          letter-spacing: 0.5px;
          color: #64748B;
        }

        .flags-list {
          display: flex;
          flex-direction: column;
          gap: 6px;
        }

        .flag-item {
          display: flex;
          align-items: flex-start;
          gap: 8px;
          padding: 8px 10px;
          border-radius: 8px;
          font-size: 11.5px;
          line-height: 1.35;
        }

        .flag-danger {
          background: #FEF2F2;
          color: #991B1B;
          border: 1px solid #FECACA;
        }

        .flag-warning {
          background: #FFFBEB;
          color: #92400E;
          border: 1px solid #FDE68A;
        }

        .flag-success {
          background: #F0FDF4;
          color: #166534;
          border: 1px solid #BBF7D0;
        }

        .panel-footer {
          padding: 14px 20px;
          border-top: 1px solid #E2E8F0;
          background: #F8FAFC;
          display: flex;
          flex-direction: column;
          gap: 8px;
          flex-shrink: 0;
        }

        .action-btn-primary {
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 6px;
          padding: 10px;
          border-radius: 8px;
          background: #2563EB;
          color: #FFFFFF;
          font-size: 13px;
          font-weight: 600;
          border: none;
          cursor: pointer;
          text-decoration: none;
        }

        .action-btn-primary:hover {
          background: #1D4ED8;
        }

        .action-btn-secondary {
          display: flex;
          align-items: center;
          justify-content: center;
          padding: 8px 10px;
          border-radius: 6px;
          background: #FFFFFF;
          color: #475569;
          font-size: 12px;
          font-weight: 500;
          border: 1px solid #CBD5E1;
          cursor: pointer;
        }

        .action-btn-secondary:hover {
          background: #F1F5F9;
          color: #0F172A;
        }

        .error-card {
          background: #FEF2F2;
          border: 1px solid #FECACA;
          border-radius: 10px;
          padding: 16px;
          display: flex;
          flex-direction: column;
          gap: 10px;
        }

        .error-title {
          font-size: 13px;
          font-weight: 700;
          color: #991B1B;
          display: flex;
          align-items: center;
          gap: 6px;
        }

        .error-msg {
          font-size: 12px;
          color: #B91C1C;
          line-height: 1.4;
        }

        @keyframes spin {
          from { transform: rotate(0deg); }
          to { transform: rotate(360deg); }
        }
      `;

      this.shadowRoot.appendChild(style);
    }

    showLoading(emailMeta, stageLabel = 'Analyzing email...', percent = 15) {
      this.ensureHost();
      this.applyStyles();

      let container = this.shadowRoot.querySelector('.panel-container');
      if (!container) {
        container = document.createElement('div');
        container.className = 'panel-container open';
        this.shadowRoot.appendChild(container);
      } else {
        container.classList.add('open');
      }

      container.innerHTML = `
        <div class="panel-header">
          <div class="brand-group">
            <div class="brand-icon">
              <svg viewBox="0 0 24 24"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
            </div>
            <div class="brand-text">
              <h2>Email Forensics</h2>
              <span>Analyzing...</span>
            </div>
          </div>
          <button class="close-btn" title="Close">&times;</button>
        </div>

        <div class="panel-body">
          <div class="context-card">
            <div class="context-subject">${escapeHtml(emailMeta?.subject || 'Analyzing Email...')}</div>
            <div class="context-meta">
              <div class="meta-line"><span class="meta-label">From:</span><span class="meta-val">${escapeHtml(emailMeta?.from || '—')}</span></div>
              <div class="meta-line"><span class="meta-label">Date:</span><span class="meta-val">${escapeHtml(emailMeta?.date || '—')}</span></div>
            </div>
          </div>

          <div class="progress-card">
            <div class="progress-header">
              <div class="progress-spinner"></div>
              <span>${escapeHtml(stageLabel)}</span>
            </div>
            <div class="progress-bar-bg">
              <div class="progress-bar-fill" style="width: ${percent}%;"></div>
            </div>
            <div class="progress-desc">Running forensic analysis...</div>
          </div>
        </div>

        <div class="panel-footer">
          <button class="action-btn-secondary close-btn-footer">Cancel</button>
        </div>
      `;

      this.bindCloseButtons(container);
    }

    showError(emailMeta, errorMessage, onRetry = null) {
      this.ensureHost();
      this.applyStyles();

      let container = this.shadowRoot.querySelector('.panel-container');
      if (!container) {
        container = document.createElement('div');
        container.className = 'panel-container open';
        this.shadowRoot.appendChild(container);
      } else {
        container.classList.add('open');
      }

      container.innerHTML = `
        <div class="panel-header">
          <div class="brand-group">
            <div class="brand-icon" style="background: #DC2626;">
              <svg viewBox="0 0 24 24"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
            </div>
            <div class="brand-text">
              <h2>Email Forensics</h2>
              <span>Notice</span>
            </div>
          </div>
          <button class="close-btn" title="Close">&times;</button>
        </div>

        <div class="panel-body">
          <div class="context-card">
            <div class="context-subject">${escapeHtml(emailMeta?.subject || 'Email Analysis')}</div>
            <div class="context-meta">
              <div class="meta-line"><span class="meta-label">From:</span><span class="meta-val">${escapeHtml(emailMeta?.from || '—')}</span></div>
            </div>
          </div>

          <div class="error-card">
            <div class="error-title">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
              <span>Message</span>
            </div>
            <div class="error-msg">${escapeHtml(errorMessage || 'An error occurred during analysis.')}</div>
          </div>
        </div>

        <div class="panel-footer">
          ${onRetry ? '<button class="action-btn-primary retry-btn">Try Again</button>' : ''}
          <button class="action-btn-secondary close-btn-footer">Close</button>
        </div>
      `;

      this.bindCloseButtons(container);

      if (onRetry) {
        const retryBtn = container.querySelector('.retry-btn');
        if (retryBtn) {
          retryBtn.addEventListener('click', onRetry);
        }
      }
    }

    showResults(analysisResult) {
      this.ensureHost();
      this.applyStyles();

      const src = analysisResult?.analysisData || analysisResult || {};
      const investigation = src.investigation || {};
      const headers = src.headers || {};
      const urls = Array.isArray(src.urls) ? src.urls : [];
      const qrCodes = Array.isArray(src.qrCodes) ? src.qrCodes : [];
      const contentAnalysis = src.contentAnalysis || {};
      const riskBreakdown = src.riskBreakdown || {};
      const correlations = Array.isArray(src.correlations) ? src.correlations : [];
      const aiAssessment = src.aiAssessment || null;
      const dashboardViewUrl = analysisResult.dashboardViewUrl || src.dashboardViewUrl || '';

      const parsedEmail = investigation?.parsedEmail || {};
      const subject = parsedEmail.subject || '(No Subject)';
      const from = parsedEmail.from || headers?.fromAddress || '—';
      const to = Array.isArray(parsedEmail.to) ? parsedEmail.to.join(', ') : (parsedEmail.to || '—');
      const date = parsedEmail.sentDate || parsedEmail.rawDate || '—';

      const riskScore = riskBreakdown?.deterministicRiskScore !== undefined ? riskBreakdown.deterministicRiskScore : 0;
      const riskLevel = (riskBreakdown?.riskLevel || 'LOW').toUpperCase();
      const confidenceScore = riskBreakdown?.evidenceConfidenceScore !== undefined ? riskBreakdown.evidenceConfidenceScore : 0;

      let badgeClass = 'badge-low';
      if (riskLevel === 'CRITICAL') badgeClass = 'badge-critical';
      else if (riskLevel === 'HIGH') badgeClass = 'badge-high';
      else if (riskLevel === 'MODERATE') badgeClass = 'badge-moderate';

      const flags = [];
      if (headers?.identityMismatchFlags && headers.identityMismatchFlags.length > 0) {
        headers.identityMismatchFlags.forEach((flag) => {
          flags.push({ type: 'danger', text: flag });
        });
      }
      if (contentAnalysis?.urgency) flags.push({ type: 'danger', text: 'Artificial urgency detected in email text' });
      if (contentAnalysis?.credentialRequest) flags.push({ type: 'danger', text: 'Password or login credential request detected' });
      if (contentAnalysis?.paymentInstruction) flags.push({ type: 'danger', text: 'Payment or wire transfer instruction detected' });
      if (contentAnalysis?.impersonation) flags.push({ type: 'warning', text: 'Authority impersonation detected' });
      if (contentAnalysis?.promptInjectionDetected) flags.push({ type: 'danger', text: 'Hidden prompt injection detected in message' });

      const suspiciousUrls = urls.filter((u) => (u.urlRiskScore && u.urlRiskScore >= 40) || u.lookalike || u.anchorMismatch);
      if (suspiciousUrls.length > 0) {
        flags.push({ type: 'danger', text: `${suspiciousUrls.length} suspicious or lookalike link(s) found` });
      }

      if (qrCodes && qrCodes.length > 0) {
        flags.push({ type: 'warning', text: `${qrCodes.length} QR code(s) found in email images` });
      }

      if (correlations && correlations.length > 0) {
        flags.push({ type: 'warning', text: `Matched ${correlations.length} known indicator(s) in database` });
      }

      let container = this.shadowRoot.querySelector('.panel-container');
      if (!container) {
        container = document.createElement('div');
        container.className = 'panel-container open';
        this.shadowRoot.appendChild(container);
      } else {
        container.classList.add('open');
      }

      container.innerHTML = `
        <div class="panel-header">
          <div class="brand-group">
            <div class="brand-icon">
              <svg viewBox="0 0 24 24"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
            </div>
            <div class="brand-text">
              <h2>Email Forensics</h2>
              <span>Email Security Report</span>
            </div>
          </div>
          <button class="close-btn" title="Close">&times;</button>
        </div>

        <div class="panel-body">
          <div class="context-card">
            <div class="context-subject">${escapeHtml(subject)}</div>
            <div class="context-meta">
              <div class="meta-line"><span class="meta-label">From:</span><span class="meta-val">${escapeHtml(from)}</span></div>
              <div class="meta-line"><span class="meta-label">To:</span><span class="meta-val">${escapeHtml(to)}</span></div>
              <div class="meta-line"><span class="meta-label">Date:</span><span class="meta-val">${escapeHtml(date)}</span></div>
            </div>
          </div>

          <div class="risk-card">
            <div class="risk-top-row">
              <div>
                <div class="score-box">
                  <span class="score-num">${riskScore}</span>
                  <span class="score-max">/100</span>
                </div>
                <div class="score-label">Risk Score</div>
              </div>
              <span class="badge ${badgeClass}">${riskLevel} RISK</span>
            </div>

            <div class="confidence-row">
              <span>Confidence: ${confidenceScore}/100</span>
              <span>Channels: ${riskBreakdown?.evaluatedChannels?.length || 5} Checked</span>
            </div>
          </div>

          <div class="ai-card">
            <div class="ai-header">
              <div class="ai-title">
                <svg viewBox="0 0 24 24"><path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83"/></svg>
                <span>AI Summary</span>
              </div>
            </div>

            <div class="ai-summary">
              ${escapeHtml(aiAssessment?.summaryExplanation || 'No summary available.')}
            </div>

            ${
              aiAssessment?.keyIndicators && aiAssessment.keyIndicators.length > 0
                ? `
                <div style="display: flex; flex-direction: column; gap: 4px;">
                  <span style="font-size: 11px; font-weight: 700; color: #475569; text-transform: uppercase;">Key Indicators:</span>
                  <ul class="ai-indicators-list">
                    ${aiAssessment.keyIndicators.map((ind) => `<li class="ai-indicator-item">${escapeHtml(ind)}</li>`).join('')}
                  </ul>
                </div>
                `
                : ''
            }
          </div>

          <div class="findings-section">
            <span class="section-label">Security Findings</span>
            <div class="flags-list">
              ${
                flags.length > 0
                  ? flags
                      .map(
                        (f) => `
                    <div class="flag-item flag-${f.type}">
                      <span>${escapeHtml(f.text)}</span>
                    </div>
                  `
                      )
                      .join('')
                  : `
                    <div class="flag-item flag-success">
                      <span>✓ No immediate phishing or security flags detected</span>
                    </div>
                  `
              }
            </div>
          </div>
        </div>

        <div class="panel-footer">
          <a href="${escapeHtml(dashboardViewUrl)}" target="_blank" class="action-btn-primary">
            <span>View Full Details in Dashboard</span>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/><polyline points="15 3 21 3 21 9"/><line x1="10" y1="14" x2="21" y2="3"/></svg>
          </a>
          <button class="action-btn-secondary close-btn-footer">Close</button>
        </div>
      `;

      this.bindCloseButtons(container);
    }

    bindCloseButtons(container) {
      const closeButtons = container.querySelectorAll('.close-btn, .close-btn-footer');
      closeButtons.forEach((btn) => {
        btn.addEventListener('click', () => this.close());
      });
    }

    close() {
      if (this.shadowHost) {
        const container = this.shadowRoot.querySelector('.panel-container');
        if (container) {
          container.classList.remove('open');
        }
      }
    }
  }

  let activeEmail = null;
  let analyzeButton = null;
  let resultPanel = null;
  let messagePicker = null;
  let domObserver = null;
  let lastUrlHash = '';

  function start() {
    analyzeButton = new EmailButton(onAnalyzeClick);
    messagePicker = new MessageSelector(onMessageChosen);
    resultPanel = new ResultPanel();

    chrome.runtime.onMessage.addListener((message) => {
      if (message?.action === 'ANALYSIS_PROGRESS') {
        const { label, percent } = message.payload || {};
        if (resultPanel) {
          resultPanel.showLoading(activeEmail, label || 'Analyzing...', percent || 50);
        }
      } else if (message?.action === 'ANALYZE_TRIGGER') {
        onAnalyzeClick();
      }
    });

    checkEmailState();

    window.addEventListener('popstate', onUrlChanged);
    window.addEventListener('hashchange', onUrlChanged);

    watchDomChanges();
  }

  function onUrlChanged() {
    if (window.location.hash !== lastUrlHash) {
      lastUrlHash = window.location.hash;
      if (resultPanel) {
        resultPanel.close();
      }
      checkEmailState();
    }
  }

  function watchDomChanges() {
    let timer = null;
    domObserver = new MutationObserver(() => {
      clearTimeout(timer);
      timer = setTimeout(() => {
        checkEmailState();
      }, 200);
    });

    domObserver.observe(document.body, {
      childList: true,
      subtree: true
    });
  }

  function isValidHexId(str) {
    return typeof str === 'string' && /^[0-9a-f]{16}$/i.test(str.trim());
  }

  function convertIdToHex(str) {
    if (!str || typeof str !== 'string') return null;
    const match = str.match(/(?:thread-f:|msg-f:|^)(\d+)/i);
    if (match && match[1]) {
      try {
        return BigInt(match[1]).toString(16).padStart(16, '0');
      } catch (_) {
        return null;
      }
    }
    return null;
  }

  function isElementVisible(el) {
    if (!el) return false;
    if (el.offsetParent === null && el.tagName !== 'BODY' && el.tagName !== 'HTML') {
      return false;
    }
    const rect = el.getBoundingClientRect();
    if (rect.width === 0 && rect.height === 0) {
      return false;
    }
    const style = window.getComputedStyle(el);
    if (style.display === 'none' || style.visibility === 'hidden' || style.opacity === '0') {
      return false;
    }
    return true;
  }

  function getUrlEmailToken() {
    const hash = window.location.hash || '';
    const match = hash.match(/^#(?:[^\/]+\/)+(?:[^\/]+\/)?([a-zA-Z0-9_\-]+)$/);
    if (!match) return null;
    const token = match[1];
    if (/^p\d+$/i.test(token)) return null;
    return token;
  }

  function findActiveEmail() {
    const main = document.querySelector('[role="main"]') || document.body;

    const headings = Array.from(main.querySelectorAll('h2.hP, h2')).filter(isElementVisible);
    const visibleSubject = headings.length > 0 ? headings[0].textContent.trim() : '';

    const messageBoxes = Array.from(
      main.querySelectorAll('.adn, [data-message-id], [data-legacy-message-id]')
    ).filter(isElementVisible);

    const expandedBoxes = messageBoxes.filter((el) => {
      return el.classList.contains('ads') || Boolean(el.querySelector('.a3s')) || el.offsetHeight > 80;
    });

    const activeBox = expandedBoxes.length > 0
      ? expandedBoxes[expandedBoxes.length - 1]
      : (messageBoxes.length > 0 ? messageBoxes[0] : null);

    let messageId = null;
    if (activeBox) {
      const legacyAttr =
        activeBox.getAttribute('data-legacy-message-id') ||
        activeBox.closest('[data-legacy-message-id]')?.getAttribute('data-legacy-message-id') ||
        activeBox.querySelector('[data-legacy-message-id]')?.getAttribute('data-legacy-message-id');

      if (isValidHexId(legacyAttr)) {
        messageId = legacyAttr;
      }

      if (!messageId) {
        const msgAttr =
          activeBox.getAttribute('data-message-id') ||
          activeBox.closest('[data-message-id]')?.getAttribute('data-message-id') ||
          activeBox.querySelector('[data-message-id]')?.getAttribute('data-message-id');

        if (isValidHexId(msgAttr)) {
          messageId = msgAttr;
        } else if (msgAttr) {
          const hex = convertIdToHex(msgAttr);
          if (isValidHexId(hex)) {
            messageId = hex;
          }
        }
      }
    }

    let threadId = null;
    const threadElements = Array.from(
      main.querySelectorAll('[data-legacy-thread-id], [data-thread-perm-id]')
    ).filter(isElementVisible);

    for (const el of threadElements) {
      const legTh = el.getAttribute('data-legacy-thread-id');
      if (isValidHexId(legTh)) {
        threadId = legTh;
        break;
      }
      const permTh = el.getAttribute('data-thread-perm-id');
      if (isValidHexId(permTh)) {
        threadId = permTh;
        break;
      }
      const convTh = convertIdToHex(permTh);
      if (isValidHexId(convTh)) {
        threadId = convTh;
        break;
      }
    }

    let senderEmail = '';
    let senderName = '';
    let date = 'Today';
    if (activeBox) {
      const senderEl = activeBox.querySelector('.gD') || activeBox.querySelector('[email]');
      if (senderEl) {
        senderEmail = senderEl.getAttribute('email') || '';
        senderName = senderEl.getAttribute('name') || senderEl.textContent.trim() || '';
      }
      const dateEl = activeBox.querySelector('.g3') || activeBox.querySelector('[title*="202"]');
      if (dateEl) {
        date = dateEl.getAttribute('title') || dateEl.textContent.trim() || 'Today';
      }
    }

    const urlToken = getUrlEmailToken();
    if (!threadId && isValidHexId(urlToken)) {
      threadId = urlToken;
    }

    const isOpen = Boolean(urlToken || visibleSubject || messageId || threadId);

    return {
      isOpen,
      urlToken,
      subject: visibleSubject || 'Email Analysis',
      messageId,
      threadId: threadId || urlToken,
      senderEmail,
      senderName,
      from: senderEmail ? (senderName ? `${senderName} <${senderEmail}>` : senderEmail) : (senderName || 'Sender'),
      date,
      count: messageBoxes.length
    };
  }

  function checkEmailState() {
    const emailInfo = findActiveEmail();

    if (!emailInfo.isOpen) {
      activeEmail = null;
      if (analyzeButton && analyzeButton.isInjected()) {
        analyzeButton.remove();
      }
      return;
    }

    activeEmail = {
      subject: emailInfo.subject,
      from: emailInfo.from,
      date: emailInfo.date
    };

    if (analyzeButton && !analyzeButton.isInjected()) {
      addAnalyzeButton();
    }
  }

  function addAnalyzeButton() {
    const toolbarSelectors = [
      '.G-atb .G-Ni.J-J5-Ji',
      '.G-atb .ar5.J-J5-Ji',
      '.G-atb [gh="tm"]',
      '.G-atb',
      '.nH.aqK .G-Ni',
      '.nH.aqK',
      '[role="main"] .G-atb'
    ];

    let targetToolbar = null;
    for (const sel of toolbarSelectors) {
      const candidates = Array.from(document.querySelectorAll(sel));
      const visible = candidates.find(isElementVisible);
      if (visible) {
        targetToolbar = visible;
        break;
      }
    }

    if (targetToolbar) {
      analyzeButton.inject(targetToolbar);
    }
  }

  async function onAnalyzeClick() {
    const emailInfo = findActiveEmail();
    if (!emailInfo.isOpen) {
      alert('Please open an email before clicking Analyze.');
      return;
    }

    const emailMeta = {
      subject: emailInfo.subject,
      from: emailInfo.from,
      date: emailInfo.date
    };
    activeEmail = emailMeta;

    analyzeButton.setLoading(true, 'Connecting...');
    resultPanel.showLoading(emailMeta, 'Reading conversation messages...', 20);

    try {
      const res = await chrome.runtime.sendMessage({
        action: 'GET_THREAD_MESSAGES',
        payload: {
          threadId: emailInfo.threadId,
          messageId: emailInfo.messageId,
          subject: emailInfo.subject,
          senderEmail: emailInfo.senderEmail
        }
      });

      if (!res || !res.success) {
        if (emailInfo.messageId) {
          onMessageChosen(emailInfo.messageId, emailMeta, emailInfo);
          return;
        }
        throw new Error(res?.error || 'Could not fetch email messages from Gmail.');
      }

      const messages = res.messages || [];

      if (messages.length === 0) {
        if (emailInfo.messageId) {
          onMessageChosen(emailInfo.messageId, emailMeta, emailInfo);
          return;
        }
        throw new Error('No messages found in this Gmail conversation.');
      }

      if (messages.length === 1) {
        onMessageChosen(messages[0].id, messages[0], emailInfo);
      } else {
        let suggestedId = emailInfo.messageId;
        if (!suggestedId || !messages.some((m) => m.id === suggestedId)) {
          const match = findExpandedMessage(messages);
          suggestedId = match ? match.id : messages[messages.length - 1].id;
        }

        analyzeButton.setLoading(false);
        messagePicker.show(messages, suggestedId);
      }
    } catch (err) {
      console.error('[Email Forensics]', err);
      analyzeButton.setLoading(false);
      resultPanel.showError(emailMeta, err.message, () => onAnalyzeClick());
    }
  }

  function findExpandedMessage(messages) {
    if (!messages || messages.length === 0) return null;

    const expandedList = Array.from(
      document.querySelectorAll('[role="main"] .adn.ads, [role="main"] .adn, [role="listitem"] .adn')
    ).filter(isElementVisible);

    if (expandedList.length > 0) {
      for (let i = expandedList.length - 1; i >= 0; i--) {
        const el = expandedList[i];
        const legId =
          el.getAttribute('data-legacy-message-id') ||
          el.closest('[data-legacy-message-id]')?.getAttribute('data-legacy-message-id');

        if (legId) {
          const found = messages.find((m) => m.id === legId);
          if (found) return found;
        }

        const senderEl = el.querySelector('.gD') || el.querySelector('[email]');
        const senderEmail = senderEl?.getAttribute('email') || '';
        if (senderEmail) {
          const found = messages.find((m) => m.from && m.from.includes(senderEmail));
          if (found) return found;
        }
      }
    }

    return messages[messages.length - 1];
  }

  async function onMessageChosen(messageId, messageMeta, optEmailInfo = null) {
    if (!messageId) return;

    const currentInfo = optEmailInfo || findActiveEmail();

    const emailMeta = {
      subject: messageMeta?.subject || currentInfo.subject || 'Email Analysis',
      from: messageMeta?.from || currentInfo.from || 'Sender',
      to: messageMeta?.to || '',
      date: messageMeta?.date || currentInfo.date || 'Today'
    };
    activeEmail = emailMeta;

    analyzeButton.setLoading(true, 'Analyzing...');
    resultPanel.showLoading(emailMeta, 'Retrieving original email and analyzing...', 30);

    try {
      const response = await chrome.runtime.sendMessage({
        action: 'ANALYZE_GMAIL_MESSAGE',
        payload: {
          messageId,
          threadId: currentInfo.threadId,
          subject: emailMeta.subject
        }
      });

      analyzeButton.setLoading(false);

      if (!response || !response.success) {
        throw new Error(response?.error || 'Forensic analysis failed.');
      }

      resultPanel.showResults(response);
    } catch (err) {
      console.error('[Email Forensics]', err);
      analyzeButton.setLoading(false);
      resultPanel.showError(emailMeta, err.message, () => onMessageChosen(messageId, messageMeta, currentInfo));
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', start);
  } else {
    start();
  }
})();

(function () {
  'use strict';

  var justDeletedActive = false;

  function refreshHistory() {
    htmx.ajax('GET', '/chat/history', { target: '#sidebar-history', swap: 'outerHTML' });
  }

  function clearActiveHistory() {
    var items = document.querySelectorAll('.history-item.active');
    for (var i = 0; i < items.length; i++) {
      items[i].classList.remove('active');
    }
  }

  function scrollToBottom() {
    var list = document.getElementById('chat-messages');
    if (list) {
      list.scrollTop = list.scrollHeight;
    }
  }

  function focusInput() {
    var input = document.getElementById('chat-input');
    if (input) {
      input.focus();
    }
  }

  function highlightMentionedDocuments() {
    var sourceLabels = document.querySelectorAll('[data-source-label]');
    var documentItems = document.querySelectorAll('[data-doc-name]');
    var labels = [];

    for (var i = 0; i < sourceLabels.length; i++) {
      labels.push(sourceLabels[i].getAttribute('data-source-label'));
    }

    for (var j = 0; j < documentItems.length; j++) {
      var item = documentItems[j];
      var name = item.getAttribute('data-doc-name');
      var mentioned = false;
      for (var k = 0; k < labels.length; k++) {
        if (labels[k] === name || labels[k].indexOf(name + ' — Page ') === 0) {
          mentioned = true;
          break;
        }
      }
      item.classList.toggle('document-source-mentioned', mentioned);
    }
  }

  function addPendingMessages() {
    var input = document.getElementById('chat-input');
    var list = document.getElementById('chat-messages');
    if (!input || !list || !input.value.trim()) {
      return;
    }

    var emptyState = list.querySelector('.text-center');
    if (emptyState) {
      emptyState.remove();
    }

    var user = document.createElement('div');
    user.className = 'msg msg-user mb-3 chat-pending-user';
    user.innerHTML = '<div class="msg-bubble"></div>';
    user.querySelector('.msg-bubble').textContent = input.value.trim();
    list.appendChild(user);

    var pending = document.createElement('div');
    pending.className = 'msg msg-assistant mb-3 chat-pending-answer';
    pending.innerHTML = '<div class="msg-bubble text-muted"><span class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>Searching your sources and preparing an answer...</div>';
    list.appendChild(pending);
    scrollToBottom();
  }

  function pollDoc(docId) {
    setTimeout(function () {
      var current = document.getElementById('doc-' + docId);
      if (!current) {
        return;
      }
      htmx.ajax('GET', '/documents/' + docId + '/status', { target: '#doc-' + docId, swap: 'outerHTML' });
    }, 1200);
  }

  function startDocPolling() {
    var pending = document.querySelectorAll('[data-doc-status="UPLOADED"], [data-doc-status="PROCESSING"]');
    for (var i = 0; i < pending.length; i++) {
      var item = pending[i];
      var docId = item.getAttribute('data-doc-id');
      if (docId && item.getAttribute('data-polling') !== '1') {
        item.setAttribute('data-polling', '1');
        pollDoc(docId);
      }
    }
  }

  document.addEventListener('htmx:afterSwap', function (event) {
    var target = event.detail.target;
    if (target) {
      if (target.id === 'chat-workspace') {
        scrollToBottom();
        focusInput();
        highlightMentionedDocuments();
      }
      if (target.id && target.id.indexOf('doc-') === 0) {
        var item = document.getElementById(target.id);
        if (item) {
          var status = item.getAttribute('data-doc-status');
          if (status === 'UPLOADED' || status === 'PROCESSING') {
            pollDoc(target.id.substring(4));
          }
        }
      }
    }
  });

  document.addEventListener('htmx:beforeSwap', function (event) {
    var el = event.detail.elt;
    if (el && el.hasAttribute('hx-delete')) {
      var li = el.closest('.history-item');
      justDeletedActive = !!(li && li.classList.contains('active'));
    }
  });

  document.addEventListener('htmx:beforeRequest', function (event) {
    var el = event.detail.elt;
    if (el && el.id === 'chat-form') {
      addPendingMessages();
      var button = el.querySelector('button[type="submit"]');
      if (button) {
        button.disabled = true;
      }
    }
  });

  document.addEventListener('htmx:afterRequest', function (event) {
    var el = event.detail.elt;
    if (!el) {
      return;
    }

    if (el.id === 'chat-form' && !event.detail.successful) {
      var pending = document.querySelector('.chat-pending-answer .msg-bubble');
      if (pending) {
        pending.textContent = 'The request failed. Please try again.';
        pending.classList.add('text-danger');
      }
      var button = el.querySelector('button[type="submit"]');
      if (button) {
        button.disabled = false;
      }
      return;
    }

    if (!event.detail.successful) {
      return;
    }

    if (el.id === 'new-chat-btn') {
      clearActiveHistory();
      refreshHistory();
    }

    if (el.id === 'chat-form') {
      refreshHistory();
    }

    if (el.id === 'pdf-input') {
      el.value = '';
      startDocPolling();
    }

    if (el.hasAttribute('hx-delete')) {
      refreshHistory();
      if (justDeletedActive) {
        justDeletedActive = false;
        clearActiveHistory();
        htmx.ajax('GET', '/chat/workspace', { target: '#chat-workspace', swap: 'innerHTML' });
      }
    }
  });

  document.addEventListener('click', function (event) {
    var link = event.target.closest('.history-item > a');
    if (!link) {
      return;
    }
    clearActiveHistory();
    link.closest('.history-item').classList.add('active');
  });

  document.addEventListener('DOMContentLoaded', function () {
    startDocPolling();
    highlightMentionedDocuments();
  });
})();
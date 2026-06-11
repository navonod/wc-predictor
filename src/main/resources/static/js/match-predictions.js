function randomScore() {
    const roll = Math.random();
    if (roll < 0.35) return 0; if (roll < 0.65) return 1; if (roll < 0.83) return 2;
    if (roll < 0.93) return 3; if (roll < 0.97) return 4; if (roll < 0.985) return 5;
    if (roll < 0.993) return 6; if (roll < 0.997) return 7; if (roll < 0.999) return 8;
    if (roll < 0.9998) return 9; return 10;
}

document.querySelectorAll('.save-btn, .save-btn-sm').forEach(btn => {
    if (btn.dataset.initialized) return;
    btn.dataset.initialized = 'true';

    const matchId = btn.getAttribute('data-match-id');
    const card = btn.closest('.match-card, .card');
    if (!card) return;

    const isCompact = btn.classList.contains('save-btn-sm');
    const s1 = card.querySelector(isCompact ? '.score-input-sm:first-of-type' : '.score-input:first-of-type');
    const s2 = card.querySelector(isCompact ? '.score-input-sm:last-of-type' : '.score-input:last-of-type');
    const dice = card.querySelector(isCompact ? '.dice-btn-sm' : '.dice-btn');
    const msg = isCompact
        ? document.getElementById('smsg_' + matchId)
        : document.getElementById('msg_' + matchId);

    if (!s1 || !s2) return;

    function checkDirty() {
        const d1 = s1.value !== s1.getAttribute('data-initial');
        const d2 = s2.value !== s2.getAttribute('data-initial');
        btn.disabled = !(d1 || d2);
    }
    s1.addEventListener('input', checkDirty);
    s2.addEventListener('input', checkDirty);

    if (dice) {
        dice.addEventListener('click', () => { s1.value = randomScore(); s2.value = randomScore(); checkDirty(); });
    }

    btn.addEventListener('click', async () => {
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm" style="width:12px;height:12px"></span>';
        if (msg) msg.innerHTML = '';
        try {
            const formData = new FormData();
            formData.append('matchId', matchId);
            formData.append('team1Score', s1.value);
            formData.append('team2Score', s2.value);
            const resp = await fetch('/api/predict/match/save', { method: 'POST', body: formData });
            const result = await resp.json();
            if (result.success) {
                s1.setAttribute('data-initial', s1.value);
                s2.setAttribute('data-initial', s2.value);
                if (msg) { msg.innerHTML = '<small class="text-success"><i class="bi bi-check"></i> Saved</small>'; setTimeout(() => { msg.innerHTML = ''; }, 2000); }
            } else {
                if (msg) msg.innerHTML = '<small class="text-danger">' + result.error + '</small>';
                btn.disabled = false;
            }
        } catch (e) {
            if (msg) msg.innerHTML = '<small class="text-danger">Failed to save</small>';
            btn.disabled = false;
        }
        btn.innerHTML = '<i class="bi bi-check' + (isCompact ? '' : '-lg') + '"></i> Save';
        checkDirty();
    });
});

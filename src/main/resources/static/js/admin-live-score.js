(function() {
    document.querySelectorAll('.live-score-inputs').forEach(wrapper => {
        const matchId = wrapper.dataset.matchId;
        if (!matchId) return;

        function getInputs() {
            return {
                s1: wrapper.querySelector('[data-team="1"]'),
                s2: wrapper.querySelector('[data-team="2"]')
            };
        }

        let timer;
        const doUpdate = async () => {
            const { s1, s2 } = getInputs();
            if (!s1 || !s2) return;
            const v1 = s1.value.trim();
            const v2 = s2.value.trim();
            if (v1 === '' || v2 === '') return;
            try {
                const resp = await fetch('/admin/api/live-score', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: 'matchId=' + encodeURIComponent(matchId) + '&team1Score=' + encodeURIComponent(v1) + '&team2Score=' + encodeURIComponent(v2)
                });
                if (resp.ok) location.reload();
            } catch(e) {
                console.error('live-score update failed', e);
            }
        };

        wrapper.querySelectorAll('.live-score').forEach(inp => {
            inp.addEventListener('input', () => {
                clearTimeout(timer);
                timer = setTimeout(doUpdate, 500);
            });
            inp.addEventListener('change', doUpdate);
        });
    });
})();

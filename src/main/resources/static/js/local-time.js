document.querySelectorAll('.local-time').forEach(el => {
    const utc = el.getAttribute('data-utc');
    if (!utc) return;
    const d = new Date(utc + 'Z');
    if (isNaN(d.getTime())) return;
    const months = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
    const hh = String(d.getHours()).padStart(2, '0');
    const mm = String(d.getMinutes()).padStart(2, '0');
    el.textContent = months[d.getMonth()] + ' ' + d.getDate() + ', ' + hh + ':' + mm;
});

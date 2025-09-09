async function refresh() {
  try {
    const res = await fetch('/api/state');
    const data = await res.json();

    const last = document.getElementById('lastTick');
    if (last) last.textContent = 'Last tick: ' + data.lastTick;

    // Prices table (if exists)
    const pricesTbl = document.getElementById('prices');
    if (pricesTbl) {
      pricesTbl.innerHTML = '';
      if (data.prices && data.prices.length > 0) {
        const assets = Object.keys(data.prices[0]).filter(k => k !== 'market');
        const thead = document.createElement('thead');
        const thr = document.createElement('tr');
        thr.innerHTML = '<th>Market</th>' + assets.map(a => `<th>${a}</th>`).join('');
        thead.appendChild(thr);
        pricesTbl.appendChild(thead);

        const tbody = document.createElement('tbody');
        data.prices.forEach(row => {
          const tr = document.createElement('tr');
          tr.innerHTML = `<td>${row.market}</td>` + assets.map(a => `<td>${row[a] ?? '-'}</td>`).join('');
          tbody.appendChild(tr);
        });
        pricesTbl.appendChild(tbody);
      }
    }
  } catch (e) { console.error(e); }
}
setInterval(refresh, 1000);
refresh();

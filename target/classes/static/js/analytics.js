async function loadMetrics() {
  const url = new URL(window.location.href);
  const period = url.searchParams.get('period'); // e.g., today
  const api = new URL(window.location.origin + '/api/metrics');
  if (period) api.searchParams.set('period', period);

  const res = await (window.fetchApi ? window.fetchApi() : fetch(api));
  const data = await res.json ? await res.json() : data; // window.fetchApi returns JSON already

  document.getElementById('range') && (document.getElementById('range').textContent = `範囲: ${data.from} 〜 ${data.to} / trades: ${data.count}`);

  // 全体累積
  const labels = data.cumulativePnL.map(p => p.ts);
  const values = data.cumulativePnL.map(p => p.value);
  new Chart(document.getElementById('cum').getContext('2d'), {
    type: 'line',
    data: { labels, datasets: [{ label: '累積PnL', data: values }] },
    options: { animation: false, responsive: true, scales: { y: { beginAtZero: true } } }
  });

  // 銘柄別
  const datasets = Object.entries(data.perAssetCumulative).map(([sym, arr]) => ({
    label: sym, data: arr.map(p => p.value),
  }));
  const assetLabels = (() => {
    let maxArr = [];
    for (const arr of Object.values(data.perAssetCumulative)) if (arr.length > maxArr.length) maxArr = arr;
    return maxArr.map(p => p.ts);
  })();
  new Chart(document.getElementById('perAsset').getContext('2d'), {
    type: 'line',
    data: { labels: assetLabels, datasets },
    options: { animation: false, responsive: true, scales: { y: { beginAtZero: true } } }
  });
}
loadMetrics().catch(console.error);

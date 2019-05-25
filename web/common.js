const COLORS = [
  '#f44336',
  '#9c27b0',
  '#3f51b5',
  '#03a9f4',
  '#009688',
  '#8bc34a',
  '#ffeb3b',
  '#ff9800',
  '#795548',
  '#607d8b',
  '#e91e63',
  '#673ab7',
  '#2196f3',
  '#00bcd4',
  '#4caf50',
  '#cddc39',
  '#ffc107',
  '#ff5722',
  '#ef9a9a',
  '#ce93d8',
  '#9fa8da',
];

let colorIndex = 0;
function getRandomColor() {
  return COLORS[colorIndex++ % COLORS.length];
}

function getColor(index) {
  index = Number(index);
  if (Number.isNaN(index)) {
    index = Math.floor(Math.random() * COLORS.length);
  }
  return COLORS[Number(index) % COLORS.length];
}

async function fetchData() {
  const search = location.search.slice(1).split('&');
  const file = search[0] ? search[0].split('=')[1] : 'uds.json';
  const res = await fetch(`/outputs/${file}`);
  return res.json();
}
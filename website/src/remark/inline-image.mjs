import { visit } from 'unist-util-visit';

function ensureData(node) {
  if (!node.data) node.data = {};
  if (!node.data.hProperties) node.data.hProperties = {};
}

export default function remarkInlineImage(options = {}) {
  const className = options.className || 'inline-img';
  const aliases = options.aliases || ['#inline'];

  return (tree) => {
    visit(tree, 'image', (node) => {
      if (!node || !node.alt) return;
      const altRaw = String(node.alt).trim();

      const matched = aliases.find((t) =>
        altRaw === t || altRaw.startsWith(t + ' ') || altRaw.startsWith(t + ':')
      );
      if (!matched) return;

      // derive alt text after the token
      let altText = '';
      if (altRaw === matched) {
        altText = '';
      } else {
        altText = altRaw.replace(new RegExp(`^${matched}[:\\s]*`), '').trim();
      }
      node.alt = altText;

      ensureData(node);
      const hp = node.data.hProperties;

      // merge existing classes
      const existing =
        hp.className ?? hp.class ?? hp['class-list'] ?? [];
      const existingArray = Array.isArray(existing)
        ? existing
        : String(existing).split(/\s+/).filter(Boolean);

      if (!existingArray.includes(className)) existingArray.push(className);

      hp.className = existingArray;
      hp.class = existingArray.join(' ');

      node.data.hProperties = hp;
    });
  };
}

import { visit } from 'unist-util-visit';

function ensureData(node) {
  if (!node.data) node.data = {};
  if (!node.data.hProperties) node.data.hProperties = {};
}

function parseHashAttributes(text) {
  const attrs = {};
  // Pattern: #key or #key=value where value may be double quoted, single quoted, or unquoted (until whitespace or next #)
  const attrRegex = /#([a-zA-Z_:\-][a-zA-Z0-9_:\-\.]*)(?:=("(?:[^"\\]|\\.)*"|'(?:[^'\\]|\\.)*'|[^\s#]+))?/g;
  let match;
  const indicesToRemove = [];

  while ((match = attrRegex.exec(text)) !== null) {
    const fullMatch = match[0];
    const key = match[1];
    let rawVal = match[2];

    // Mark this match region for removal later
    indicesToRemove.push([match.index, match.index + fullMatch.length]);

    if (rawVal === undefined) {
      // No "=value" present -> boolean true
      attrs[key] = true;
      continue;
    }

    // Trim surrounding quotes if present
    if ((rawVal.startsWith('"') && rawVal.endsWith('"')) ||
        (rawVal.startsWith("'") && rawVal.endsWith("'"))) {
      rawVal = rawVal.slice(1, -1).replace(/\\(["'])/g, '$1'); // unescape simple \" or \'
    }

    // Try to coerce numeric values to Number
    if (/^-?\d+(\.\d+)?$/.test(rawVal)) {
      attrs[key] = rawVal.includes('.') ? parseFloat(rawVal) : parseInt(rawVal, 10);
    } else {
      attrs[key] = rawVal;
    }
  }

  // Remove matched attribute tokens from the text
  if (indicesToRemove.length) {
    let out = '';
    let last = 0;
    for (const [start, end] of indicesToRemove) {
      out += text.slice(last, start);
      // replace removed section with a single space so words don't run together
      out += ' ';
      last = end;
    }
    out += text.slice(last);
    const cleaned = out.replace(/\s+/g, ' ').trim();
    return { attrs, cleanedText: cleaned };
  }

  return { attrs, cleanedText: text.trim() };
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

      // parse #key=value tokens from the remaining altText
      const { attrs, cleanedText } = parseHashAttributes(altText);

      // set the cleaned alt
      node.alt = cleanedText;

      ensureData(node);
      const hp = node.data.hProperties;

      // merge existing classes
      const existing =
        hp.className ?? hp.class ?? hp['class-list'] ?? [];
      const existingArray = Array.isArray(existing)
        ? existing
        : String(existing).split(/\s+/).filter(Boolean);

      if (!existingArray.includes(className)) existingArray.push(className);

      // if attrs contains a class or className, merge them in
      if (attrs.class || attrs.className) {
        const attrClassValue = attrs.class ?? attrs.className;
        const attrClassArray = Array.isArray(attrClassValue)
          ? attrClassValue
          : String(attrClassValue).split(/\s+/).filter(Boolean);
        for (const c of attrClassArray) {
          if (c && !existingArray.includes(c)) existingArray.push(c);
        }

        delete attrs.class;
        delete attrs.className;
      }

      hp.className = existingArray;
      hp.class = existingArray.join(' ');

      // Apply parsed attributes into hProperties
      // Prefer to not overwrite existing explicit hProperties unless they are absent
      for (const [k, v] of Object.entries(attrs)) {
        // If there's already an hProperty present, do not override it
        if (hp[k] === undefined) {
          hp[k] = v;
        }
      }

      node.data.hProperties = hp;
    });
  };
}

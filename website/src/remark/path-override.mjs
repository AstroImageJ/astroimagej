import { visit } from 'unist-util-visit';
import { parseFragment, serialize } from 'parse5';

/**
 * Rewrites inline html img tag src's with relative paths to the public folder to instead refer
 * to the files directly. This is used so that Idea's markdown preview will correctly display the image
 */
export default function remarkRewriteInlineHtmlImgs(options = {}) {
  return (tree, file) => {
    visit(tree, 'html', (node) => {
      if (!node || !node.value || typeof node.value !== 'string') return;

      // Quick fast-path: if the html doesn't contain "img" or publicFolder, skip
      if (!/img/i.test(node.value) || !new RegExp(`public`, 'i').test(node.value)) {
        return;
      }

      try {
        // parse the HTML fragment (safe for inline HTML)
        const fragment = parseFragment(node.value);
        //console.error('[remark-rewrite] visiting fragment', fragment);
        walkAndRewrite(fragment);
        const rebuilt = serialize(fragment);
        // Only replace when changed (helps keep source maps stable)
        if (rebuilt !== node.value) {
          node.value = rebuilt;
        }
      } catch (err) {
        // parsing failure: do not throw — just leave node as-is
        // (we could log if desired)
        console.error(err)
        return;
      }
    });
  };
}

// Rewrite a single URL if it references the public folder.
  function rewriteUrl(url) {
    if (!url || typeof url !== 'string') return url;
    const trimmed = url.trim();

    // Don't rewrite absolute/protocol URLs
    if (/^[a-zA-Z][a-zA-Z0-9+.-]*:\/\//.test(trimmed) || trimmed.startsWith('//')) {
        console.error('Skipping...');
      return trimmed;
    }

    // Match optional ./ or ../ or leading / then publicFolder/
    const regex = new RegExp(`^(?:\.\.\/)+public\\/(.*)$`);
    const m = trimmed.match(regex);
    if (m) {
      // return root-relative path
      return '/' + m[1];
    }
    return trimmed;
  }

  function walkAndRewrite(node) {
    if (!node) return;
    if (Array.isArray(node.childNodes)) {
      for (const child of node.childNodes) {
        walkAndRewrite(child);
      }
    }

    // parse5 element nodes have tagName and attrs
    if (node.tagName && node.tagName.toLowerCase() === 'img' && Array.isArray(node.attrs)) {
      for (const attr of node.attrs) {
        if (attr.name.toLowerCase() === 'src') {
          attr.value = rewriteUrl(attr.value);
        }
      }
    }
  }
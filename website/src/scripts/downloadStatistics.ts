const GITHUB_API =
  "https://api.github.com/repos/AstroImageJ/AstroImageJ/releases";

export async function getReleaseStatistics(): Promise<any> {
  const collected: any[] = [];

  const perPage = 100;
  let nextUrl: string | null = `${GITHUB_API}?per_page=${perPage}`;

  // Load all releases, following pagination
  while (nextUrl) {
    const res = await fetch(nextUrl);
    if (!res.ok) {
      const details = await res.text().catch(() => "");
      return new Response(
        JSON.stringify({ error: `GitHub API returned ${res.status}`, details }),
        { status: 502, headers: { "content-type": "application/json" } },
      );
    }
    const page = await res.json();
    if (Array.isArray(page)) collected.push(...page);
    // parse Link header for next page
    const link = res.headers.get("link");
    const parsed = parseLinkHeader(link);
    nextUrl = parsed.next ?? null;
  }

  const releases = collected.map((r: any) => {
    const assets = (r.assets || []).map((a: any) => ({
      name: a.name,
      download_count: a.download_count,
      size: a.size,
      target: parseArtifactName(a.name),
    }));
    const total_downloads = assets.reduce(
      (s: number, a: any) => s + (a.download_count || 0),
      0,
    );
    return {
      name: r.name || r.tag_name,
      published_at: r.published_at,
      html_url: r.html_url,
      total_downloads,
      assets,
    };
  });

  return releases.filter((r) => r.name.startsWith("6"));
}

function parseLinkHeader(header: string | null): Record<string, string> {
  if (!header) return {};
  const parts = header.split(",").map((p) => p.trim());
  const links: Record<string, string> = {};
  for (const part of parts) {
    const m = part.match(/<([^>]+)>;\s*rel="([^"]+)"/);
    if (m) links[m[2]] = m[1];
  }
  return links;
}

function parseArtifactName(name: string): string {
  var os: Os | undefined;
  if (name.includes("-mac-")) {
    os = Os.MAC;
  } else if (name.includes("-windows-")) {
    os = Os.WINDOWS;
  } else if (name.includes("-linux-")) {
    os = Os.LINUX;
  }

  var arch: Arch | undefined;
  if (name.includes("-x64")) {
    arch = Arch.x64;
  } else if (name.includes("-aarch64")) {
    arch = Arch.ARM;
  }

  return os?.toString() + " " + arch?.toString();
}

enum Os {
  MAC = "Mac",
  WINDOWS = "Windows",
  LINUX = "Linux",
}

enum Arch {
  x64 = "x64",
  ARM = "Arm",
}

/* getReleaseStatistics().then((r) => {
  console.log(r);
  console.log(r.reduce((s: number, a: any) => s + (a.total_downloads || 0), 0));
}); */

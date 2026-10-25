"""Sprawdza formę komentarzy wg D-9 (CLAUDE.md, docs/testing.md) w plikach z danej rewizji gita.

Detekt nie ma reguły na formę komentarza (KDoc nad deklaracją zamiast `//`), dlatego skrypt chodzi w CI
obok detekta. Kod wyjścia 1, gdy są uwagi.

Użycie: python3 -I -X utf8 tools/kdoc_form.py <repo> <rev> [prefiks-ścieżki ...]   (CI: `. HEAD`)
Raportuje:
  LINE   – komentarz `//` w zasięgu deklaracji (plik, ciało klasy/obiektu, parametry konstruktora),
           czyli poza ciałem funkcji; wyjątki: `// TODO(KAFE-…)`, w testach `// given/when/then`
           i znaczniki sekcji `// --- … ---` między grupami testów.
  TRAIL  – `//` na końcu linii kodu w zasięgu deklaracji.
  BLOCK  – komentarz `/* … */` (nie KDoc) bezpośrednio nad deklaracją.
  TICKET – numer zgłoszenia „KAFE-n” w dowolnym komentarzu (także w ciele funkcji, w testach, w .kts, XML,
           YAML, TOML); wyjątki: `TODO(KAFE-n)` i `@Ignore // KAFE-n BUG-m` (D-9, 2026-10-18).
  NOKDOC – efektywnie publiczna deklaracja (bez override) bez KDoc bezpośrednio nad nią (adnotacje pomijane).
           Pole konstruktora liczy się jako opisane, gdy KDoc klasy ma `@property <nazwa>` lub `@param <nazwa>`.
           Zbiory testowe są pomijane (D-9: testy bez komentarzy).
Heurystyka bez pełnego parsera Kotlina – wynik do przejrzenia.
"""
import re
import subprocess
import sys

MODS = (r'public|private|internal|protected|open|abstract|override|data|sealed|enum|inline|value|const|lateinit|'
        r'suspend|operator|infix|tailrec|external|annotation|inner|companion|expect|actual|final')
DECL_RE = re.compile(rf'^((?:{MODS}|fun(?=\s+interface))\s+)*'
                     r'(class|interface|object|fun|val|var|typealias|constructor)\b\s*(?:<[^>]*>\s*)?([A-Za-z_`][\w`.]*)?')
ANN = re.compile(r'^(@[\w.:]+(\([^)]*\))?\s*)+')
TEST_SETS = ('commonTest', 'androidHostTest', 'androidDeviceTest', 'iosTest', '/test/')
NON_PUBLIC = re.compile(r'\b(private|internal|protected)\b')
TICKET_RE = re.compile(r'KAFE-\d+')
block_texts = []        # (linia_start, pełny tekst komentarza blokowego) – wypełnia scan()


def ticket_findings(line_comments, blocks_with_text, code_lines):
    """TICKET – numer zgłoszenia w komentarzu (D-9, 2026-10-18). Wyjątki: `TODO(KAFE-x)` i `@Ignore // KAFE-x BUG-y`."""
    out = []
    for ln, (txt, _) in sorted(line_comments.items()):
        if not TICKET_RE.search(txt):
            continue
        if re.match(r'TODO\(KAFE-\d+\)', txt):
            continue
        code = code_lines[ln] if ln < len(code_lines) else ''
        if '@Ignore' in code and re.match(r'KAFE-\d+ BUG-\d+', txt):
            continue
        out.append(('TICKET', ln + 1, '// ' + txt[:100]))
    for start, text in blocks_with_text:
        for off, part in enumerate(text.split('\n')):
            if TICKET_RE.search(part) and not re.search(r'TODO\(KAFE-\d+\)', part):
                out.append(('TICKET', start + off + 1, part.strip()[:100]))
    return out


def other_file_tickets(path, src):
    """TICKET w komentarzach plików spoza Kotlina: XML `<!-- -->`, YAML/TOML/properties `#`."""
    out = []
    if path.endswith('.xml'):
        for m in re.finditer(r'<!--(.*?)-->', src, re.S):
            if TICKET_RE.search(m.group(1)):
                out.append(('TICKET', src.count('\n', 0, m.start()) + 1, '<!--' + m.group(1).strip()[:100]))
    elif path.endswith(('.yml', '.yaml', '.toml', '.properties')):
        for ln, line in enumerate(src.split('\n')):
            m = re.search(r'(^|\s)#(.*)$', line)
            if m and TICKET_RE.search(m.group(2)) and not re.match(r'\s*TODO\(KAFE-\d+\)', m.group(2)):
                out.append(('TICKET', ln + 1, '#' + m.group(2).strip()[:100]))
    return out


def scan(src):
    """code[i] = linia i bez komentarzy (literały skrócone); komentarze liniowe i blokowe z numerami linii."""
    n = len(src)
    code, cur, line, i = [], [], 0, 0
    line_comment = {}   # linia -> (tekst, czy przed komentarzem jest kod)
    blocks = []         # (linia_start, linia_koniec, czy_KDoc)
    block_texts.clear()

    def newline():
        nonlocal cur, line
        code.append(''.join(cur)); cur = []; line += 1

    while i < n:
        c = src[i]
        if c == '\n':
            newline(); i += 1
        elif src.startswith('"""', i):
            j = src.find('"""', i + 3); j = n if j < 0 else j + 3
            while j < n and src[j] == '"':
                j += 1
            cur.append('""')
            for _ in range(src.count('\n', i, j)):
                newline()
            i = j
        elif c in '"\'':
            j = i + 1
            while j < n and src[j] != c and src[j] != '\n':
                j += 2 if src[j] == '\\' else 1
            cur.append(c + c); i = j + 1
        elif src.startswith('//', i):
            j = src.find('\n', i); j = n if j < 0 else j
            line_comment[line] = (src[i + 2:j].strip(), bool(''.join(cur).strip()))
            i = j
        elif src.startswith('/*', i):
            kdoc = src.startswith('/**', i) and not src.startswith('/**/', i)
            depth, j, start = 1, i + 2, line
            while j < n and depth:
                if src.startswith('/*', j):
                    depth += 1; j += 2
                elif src.startswith('*/', j):
                    depth -= 1; j += 2
                else:
                    if src[j] == '\n':
                        newline()
                    j += 1
            blocks.append((start, line, kdoc))
            block_texts.append((start, src[i:j]))
            cur.append(' '); i = j
        else:
            cur.append(c); i += 1
    code.append(''.join(cur))
    return code, line_comment, blocks


def scopes(code):
    """Zasięg na początku każdej linii: ('decl'|'params'|'code', efektywnie_public, nazwa_typu)."""
    stack = [('decl', True, None)]
    at_line, header = [], ''
    for text in code:
        at_line.append(stack[-1])
        for ch in text:
            kind, pub, _ = stack[-1]
            if ch in '{(':
                h = ANN.sub('', header.strip())
                m = DECL_RE.match(h) if kind == 'decl' else None
                is_type = bool(m and m.group(2) in ('class', 'interface', 'object'))
                vis_pub = pub and not (m and NON_PUBLIC.search(m.group(0)))
                if ch == '{':
                    stack.append(('decl', vis_pub, m.group(3)) if is_type else ('code', pub, None))
                    header = ''
                else:
                    first = '(' not in h
                    stack.append(('params', vis_pub, m.group(3)) if is_type and first else ('code', pub, None))
                    header += ch
            elif ch in '})':
                if len(stack) > 1:
                    stack.pop()
                header = '' if ch == '}' else header + ch
            elif ch == ';':
                header = ''
            else:
                header += ch
        s = text.strip()
        continued = s.endswith((',', '(', '=', ':', '->', '.')) or (s and ANN.fullmatch(s + ' '))
        if stack[-1][0] == 'decl' and s and not continued:
            header = ''
        else:
            header += '\n'
    return at_line


def analyse(path, src):
    is_test = any(t in path for t in TEST_SETS)
    code, line_comment, blocks = scan(src)
    at_line = scopes(code)
    lines = src.split('\n')
    block_end = {b[1]: b for b in blocks}
    in_block = set()
    for s, e, _ in blocks:
        in_block.update(range(s, e + 1))

    def above(ln):
        """Indeks linii bezpośrednio nad ln po pominięciu adnotacji (także wielolinijkowych, np. `@Database(...)`)."""
        k = ln - 1
        while k >= 0 and k not in block_end:
            s = code[k].strip()
            if s.startswith('@'):
                k -= 1
                continue
            if s.endswith((')', '),')):
                depth, j = 0, k
                while j >= 0:
                    depth += code[j].count(')') - code[j].count('(')
                    if depth <= 0:
                        break
                    j -= 1
                if 0 <= j < k and code[j].strip().startswith('@'):
                    k = j - 1
                    continue
            break
        return k

    def kdoc_above(ln):
        b = block_end.get(above(ln))
        return b if b and b[2] else None

    findings = ticket_findings(line_comment, list(block_texts), code)
    if path.endswith('.kts'):
        return sorted(findings, key=lambda f: f[1])
    for ln, (txt, has_code) in sorted(line_comment.items()):
        if ln < len(at_line) and at_line[ln][0] == 'code':
            continue
        if re.match(r'TODO\(KAFE-\d+\)', txt) or (is_test and re.match(r'(given|when|then)\b|---.*---$', txt, re.I)):
            continue
        if '@Ignore' in code[ln] and re.match(r'KAFE-\d+ BUG-\d+', txt):
            continue
        findings.append(('TRAIL' if has_code else 'LINE', ln + 1, '// ' + txt[:100]))

    for ln, text in enumerate(code):
        kind, pub, owner = at_line[ln]
        if kind not in ('decl', 'params') or (ln in in_block and not text.strip()):
            continue
        m = DECL_RE.match(ANN.sub('', text.strip()))
        if not m:
            continue
        what, name, mods = m.group(2), m.group(3) or '', m.group(0)
        if kind == 'params' and what not in ('val', 'var'):
            continue
        b = block_end.get(above(ln))
        if b and not b[2]:
            findings.append(('BLOCK', ln + 1, f'/* */ nad {what} {name}'))
        if is_test or not pub or NON_PUBLIC.search(mods) or re.search(r'\boverride\b', mods):
            continue
        if kdoc_above(ln):
            continue
        if kind == 'params':
            hdr = ln
            while hdr >= 0 and at_line[hdr][0] == 'params':
                hdr -= 1
            kd = kdoc_above(hdr)
            if kd and re.search(rf'@(property|param)\s+{re.escape(name)}\b', '\n'.join(lines[kd[0]:kd[1] + 1])):
                continue
        findings.append(('NOKDOC', ln + 1, f'{what} {name}' + (f'  (w {owner})' if owner else '')))
    return sorted(findings, key=lambda f: f[1])


def main():
    repo, rev, prefixes = sys.argv[1], sys.argv[2], sys.argv[3:] or ['']
    files = subprocess.run(['git', '-C', repo, 'ls-tree', '-r', '--name-only', rev],
                           capture_output=True, text=True, encoding='utf-8').stdout.split()
    total = 0
    for f in files:
        if not any(f.startswith(p) for p in prefixes):
            continue
        kotlin = f.endswith(('.kt', '.kts'))
        if not kotlin and not f.endswith(('.xml', '.yml', '.yaml', '.toml', '.properties')):
            continue
        if not kotlin and '/schemas/' in f:
            continue
        src = subprocess.run(['git', '-C', repo, 'show', f'{rev}:{f}'], capture_output=True).stdout.decode('utf-8', 'replace')
        src = src.replace('\r\n', '\n')
        found = analyse(f, src) if kotlin else other_file_tickets(f, src)
        for kind, ln, msg in found:
            print(f'{kind:6} {re.sub(r"^.*?/kotlin/com/karolina/cieslik/kafe/", "", f)}:{ln}  {msg}')
            total += 1
    print(f'-- {total} uwag')
    return 1 if total else 0


if __name__ == '__main__':
    sys.exit(main())

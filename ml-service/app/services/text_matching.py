"""Shared word-boundary substring check, used by both the stub LLM's skill
detection and the matcher's skill derivation/matching.

Boundaries are defined as "not adjacent to a letter or digit" rather than
regex's own ``\\b`` or a wider punctuation-inclusive class — that's the only
definition that gets every case in this vocabulary right at once: it stops
"r" matching inside "for", but still finds "react" at the end of a sentence
("...and React.") and "c#"/"c++"/".net" regardless of the punctuation
touching them, none of which plain ``\\b`` (which treats +, #, . as
non-word) or a stricter "exclude .+#-" class (which blocks a following
period) handle correctly for this mixed alnum/symbol vocabulary.
"""

import re


def word_in_text(phrase: str, lower_text: str) -> bool:
    """``lower_text`` must already be lowercased; ``phrase`` is lowercased here."""
    return re.search(
        rf"(?<![A-Za-z0-9]){re.escape(phrase.lower())}(?![A-Za-z0-9])", lower_text,
    ) is not None

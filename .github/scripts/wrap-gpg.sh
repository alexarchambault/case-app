#!/usr/bin/env sh

mkdir -p ~/mybin
touch ~/mybin/gpg
chmod +x ~/mybin/gpg
echo '#!/usr/bin/env sh' > ~/mybin/gpg
echo "$(which gpg) --no-tty --yes \"\$@\"" >> ~/mybin/gpg
echo "$HOME/mybin" >> $GITHUB_PATH

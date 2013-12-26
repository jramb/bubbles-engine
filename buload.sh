echo "Shell args: $*"
echo "BUBBLE-ENV=$BUBBLE_URL $BUBBLE_DOMAIN"

#java -cp "lib/*:src:classes:static:." bubbles.buload $*

echo ./bubbles buload -d $BUBBLE_DOMAN -b $BUBBLE_URL $*
./bubbles buload -d $BUBBLE_DOMAN -b $BUBBLE_URL $*

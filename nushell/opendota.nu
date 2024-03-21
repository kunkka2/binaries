
const IMGURL = "https://cdn.cloudflare.steamstatic.com"
const TMP = "tmp"
const DIST = "dist"
const FDIR = "build/"
def cmd_7z [ ...args ] {
    if 'CMD_7Z' in $env {
        let app = $args| str join ' '
        nu -c $"($env.CMD_7Z) ($app)"
    } else {
        7z ...$args
    }
}

def http_get [ url, ttname ] {
    
    if not ( 'NOREQ' in $env ) {
        http get $url | save -f $ttname
    }
    return (open $ttname)
}
def http_save [ url, ttname ] {
    
    if not ( 'NOREQ' in $env ) {
        http get $url | save -f $ttname
    }
}
def http_try_save [ url, ttname ] {
    
    if not ( 'NOREQ' in $env ) {
        try {
            http get $url | save -f $ttname
        } catch { 
            print ( 'get faild' + $url )
        }
        
    }
}
def option_zip [url,ttname] {
    let obj = $ttname|path parse
    let dir = $obj.parent
    let stem = $obj.stem
    let base = $"($obj.stem).($obj.extension)"
    
    if not ( 'NOREQ' in $env ) {
        http_save https://github.com/odota/dotaconstants/archive/refs/heads/master.zip $ttname
    }
    let pwd = (pwd)
    cd $dir
    rm -fr $stem
    cmd_7z x $base
}

def main [] {
    # load all build/*.json
    if not ( $TMP | path exists) {
        mkdir $TMP
    }
    if not ( $DIST | path exists) {
        mkdir $DIST
    }
    let imgs = $"($DIST)/imgs"
    if not ( $imgs | path exists) {
        mkdir imgs
    }
    let ttname = $"($TMP)/tree.json"
    let data = ( http_get "https://api.github.com/repos/odota/dotaconstants/git/trees/master?recursive=1" $ttname )
    let zip = $"($TMP)/dotaconstants-master.zip"
    option_zip "https://github.com/odota/dotaconstants/archive/refs/heads/master.zip" $zip
    for item in ( ls $"($TMP)/dotaconstants-master/($FDIR)" ) {
        cp $item.name $"($DIST)/."
    }
    # abilities.json
    let abilities = open $"($DIST)/abilities.json"
    let columns = $abilities|columns|reverse
    if not ( $"($imgs)/abilities" | path exists) {
        mkdir $"($imgs)/abilities"
    }
    for item in $columns {
        let data = $abilities |get $item
        let img = $data|get 'img'
        let obj = $img | path parse
        let png = $"($imgs)/abilities/($item).($obj.extension)"
        http_try_save ( $IMGURL + $img ) $png
        if ( 'NOREQ' in $env ) {
            break
        }
    }
    # heroes.json
    let heroes = open $"($DIST)/heroes.json"
    let columns = $heroes|columns|reverse
    if not ( $"($imgs)/heroes" | path exists) {
        mkdir $"($imgs)/heroes"
    }
    for item in $columns {
        let data = $heroes |get $item
        let img = $data|get 'img'
        let icon = $data|get 'icon'
        let obj = $img | path parse
        let png = $"($imgs)/heroes/($item).($obj.extension)"
        http_save ( $IMGURL + $img ) $png
        let obj = $icon |path parse
        let png = $"($imgs)/heroes/($item).icon.($obj.extension)"
        http_save ( $IMGURL + $img ) $png
        if ( 'NOREQ' in $env ) {
            break
        }
    }
    # items.json
    let items = open $"($DIST)/items.json"
    let columns = $items|columns|reverse
    if not ( $"($imgs)/items" | path exists) {
        mkdir $"($imgs)/items"
    }
    for item in $columns {
        let data = $items |get $item
        let img = $data|get 'img'
        let obj = $img | path parse
        let png = $"($imgs)/items/($item).($obj.extension)"
        http_save ( $IMGURL + $img ) $png
        if ( 'NOREQ' in $env ) {
            break
        }
    }
}
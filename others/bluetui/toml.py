import tomllib
import tomli_w
with open("bluetui/Cargo.toml","r+b") as conf:
  data = tomllib.load(conf)
  conf.seek(0)
  conf.truncate()
  conf.seek(0)
  data['workspace']['members'].append("bluetui")
  tomli_w.dump(data,conf)

with open("bluetui/bluetui/Cargo.toml","r+b") as conf:
  data = tomllib.load(conf)
  conf.seek(0)
  conf.truncate()
  conf.seek(0)
  data['dependencies']['bluer']['path'] = "../bluer"
  tomli_w.dump(data,conf)

with open("bluetui/bluer/Cargo.toml","r+b") as conf:
  data = tomllib.load(conf)
  conf.seek(0)
  conf.truncate()
  conf.seek(0)
  data['dependencies']['dbus']['features'].append("vendored")
  tomli_w.dump(data,conf)


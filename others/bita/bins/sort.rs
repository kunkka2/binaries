#[allow(unused_imports)]
use std::fs::File;
use std::fs;
use std::env;
use std::path::Path;
use std::io::{ Write};
use std::net::{IpAddr, Ipv4Addr, SocketAddr};
use std::sync::Arc;
use reqwest::Proxy;
use tokio::runtime;
use tokio::time::{Instant, Duration};
use futures::future::join_all;
use tokio::sync::Mutex;

struct Ipres {
    usetime:Duration,
    ipstr:String,
}



async fn use_ip(ipport:String) -> Ipres{
    let start = Instant::now();
    let mut ipres = Ipres {
        usetime:Duration::from_secs(1343),
        ipstr: ipport.to_string(),
    };
    let proxy = Proxy::all("SOCKS5://".to_string()+&ipport);
    loop {
        if !proxy.is_ok(){
            break
        }
        let proxy = proxy.unwrap();
        let host = env::var("HOST_ADDRESS").unwrap();
        let addr_str = env::var("SOCKET_ADDRESS").unwrap();
        let addr = addr_str.parse::<SocketAddr>().unwrap();
        let clientw = reqwest::Client::builder()
            .resolve("kunkka.proxy",addr)
            .proxy(proxy)
            .timeout(Duration::from_secs(1))
            .build();
        if clientw.is_ok(){
            let client=clientw.unwrap();
            let data = client
                .get(host)
                .send().await;
            ipres.usetime=start.elapsed();
            if data.is_ok() {
                let da=data.unwrap();
                let code = da.status();
                if code!=200 {
                    ipres.ipstr="".to_string();
                }

            }else{
                ipres.ipstr="".to_string();
            }
        }else{
            ipres.ipstr="".to_string();
        }
        return ipres
    }
    

    Ipres {
        usetime:Duration::from_secs(36000),
        ipstr: "".to_string(),
    }
}
async fn ten_ok (works: &mut Arc<Mutex<Vec<Ipres>>>,sokcs: Vec<String>) {
    let mut handles = Vec::new();
    for ipstr in sokcs {
        let ips = ipstr.clone();
        handles.push(use_ip(ips));
    }
    let res = join_all(handles).await;
    let mut worksc=works.lock().await;
    for ipres in res {

        if &ipres.ipstr != "" {
            println!("add one {}",&ipres.ipstr);
            worksc.push(ipres);
        } else {
            print!(".");
        }

    }
}


async fn run(ips:Vec<String>,works:&mut Arc<Mutex<Vec<Ipres>>>){
    println!("00009");
    let lenggth = ips.len();
    let count =lenggth / 100;
    let mut idx:usize =0;
    loop {
        idx+=1;
        let mut eeee = idx*100;
        if eeee > lenggth {
            eeee = lenggth;
        }
        let mut e100=0 as usize;
        if idx > 1 {
            e100 = eeee - 100;
        }
        let data = &ips[e100..eeee];
        let mut socks:Vec<String>=vec!();
        for sitem in data{
            socks.push(sitem.clone());
        }
        ten_ok(works,socks).await;

        if idx>=count{
            println!("ten ok end!");
            break;
        }
    }
}

fn main() {
    runtime::Builder::new_multi_thread()
        .worker_threads(8)
        .enable_all()
        .build()
        .unwrap()
        .block_on(async move {
            println!("00001");
            let dddist = std::env::var("PSOCKS").unwrap_or("socks5".to_string());
            let contents = fs::read_to_string(dddist.clone()+".txt");
            let link = &(dddist + ".use.txt");
            let path = Path::new(link);
            let fileout = File::create(path);
            let works:Arc<Mutex<Vec<Ipres>>>= Arc::new(Mutex::new(Vec::new()));
            if contents.is_ok() && fileout.is_ok() {
                println!("00002");
                let mut fileout=fileout.unwrap();

                let contents = contents.unwrap();
                let lines:Vec<String>= contents.lines().map(String::from).collect();
                let length = lines.len();
                let count = length / 4;
                let linesw=lines.clone();
                let mut stepa:Vec<String>=vec!();
                for val in linesw[0..count].iter() {
                    stepa.push(val.to_string())
                }
                let linesw=lines.clone();
                let mut stpeb:Vec<String>=vec!();
                println!("00003");
                for val in linesw[count..(2*count)].iter() {
                    stpeb.push(val.to_string())
                }
                let linesw=lines.clone();
                let mut stepc:Vec<String>=vec!();
                for val in linesw[(2*count)..(3*count)].iter() {
                    stepc.push(val.to_string())
                }
                let linesw=lines.clone();
                let mut stepd:Vec<String>=vec!();
                for val in linesw[(3*count)..].iter() {
                    stepd.push(val.to_string())
                }
                println!("00004");
                let mut worksc=works.clone();
                let handa=tokio::spawn(async move{
                    println!("00005");
                    run(stepa.iter().map(|aa|aa.clone()).collect(),&mut worksc).await;
                    println!("00007");
                });
                let mut worksc=works.clone();
                let handb = tokio::spawn(async move{
                    run(stpeb.iter().map(|aa|aa.clone()).collect(),&mut worksc).await;
                });
                let mut worksc=works.clone();
                let handc = tokio::spawn(async move {
                    run(stepc.iter().map(|aa|aa.clone()).collect(),&mut worksc).await;
                });
                let mut worksc=works.clone();
                let handd = tokio::spawn(async move{
                    run(stepd.iter().map(|aa|aa.clone()).collect(),&mut worksc).await;
                });
                println!("00009");
                join_all([handa,handb,handc,handd]).await;
                println!("00008");
                let hande = tokio::spawn( async move {
                    println!("00008.1");
                    let works = works.lock().await;
                    println!("00008.2");
                    let mut works2:Vec<&Ipres> = vec!();
                    for ipres in works.iter() {
                        works2.push(ipres)
                    }
                    let mut sorted = false;
                    let length = works.len();
                    println!("00008.2 {}",length);
                    if length ==0 {
                        sorted=true;
                    }
                    while !sorted {
                        sorted = true;
                        for i in 0..(length-1) {
                            if works2[i].usetime > works2[i+1].usetime {
                                sorted = false;
                                let tmp = works2[i];
                                let tmp2 = works2[i+1];
                                works2[i] = tmp2;
                                works2[i+1]= tmp;
                            }
                        }
                    }
                    for ipres in works2 {
                        let aa = fileout.write((ipres.ipstr).as_ref());
                        if aa.is_ok() {
                            println!("+");
                        }
                        let _ = fileout.write(b"\n");
                    }
                    return
                });
                let _ = hande.await;
                //
                //tokio::task::spawn
                // let mut works = works.get_mut();
                // let mut works2:Vec<&Ipres> = vec!();
                // for ipres in works {
                //     works2.push(ipres)
                // }
                // let mut sorted = false;
                // let length = works.len();
                // if length ==0 {
                //     sorted=true;
                // }
                // while !sorted {
                //     sorted = true;
                //     for i in 0..(length-1) {
                //         if works2[i].usetime > works2[i+1].usetime {
                //             sorted = false;
                //             let tmp = works2[i];
                //             let tmp2 = works2[i+1];
                //             works2[i] = tmp2;
                //             works2[i+1]= tmp;
                //         }
                //     }
                // }
                //
                //
                // for ipres in works2 {
                //     fileout.write((ipres.ipstr).as_ref());
                //     fileout.write(b"\n");
                // }
            } else {
                println!("not run stupid");
                return
            }
            // tokio::task::spawn(async {
            //     println!("000");
            //     run(".idea/socks5.1".to_string()).await;
            //     println!("000 end");
            // });
            // tokio::task::spawn(async {
            //     println!("001");
            //     run(".idea/socks5.2".to_string()).await;
            //     println!("001 end");
            // });
            // tokio::task::spawn(async {
            //     println!("002");
            //     run(".idea/socks5.3".to_string()).await;
            //     println!("002 end");
            // });
            // tokio::task::spawn(async {
            //     println!("spawn ");
            //     abcd(".idea/socks5.4".to_string()).await;
            //     println!("spawn end");
            //     println!("003 end");
            // });
        })
}
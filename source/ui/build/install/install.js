const FtpClient = require('ftp');
const fs = require('fs');
const path = require('path');
const rimraf = require('rimraf');
const exec = require('child_process').execSync;
const envDependencies = require('./package.json').envDependencies;
const platform = require('os').platform();

const filename = `node_modules_${envDependencies.node_modules.version}.${platform === 'win32' ? 'rar' : 'tar.xz'}`;
const remote_path = `Dependence/node_modules${envDependencies.node_modules.path}/${platform}/${filename}`;

if (fs.existsSync('node_modules/.version') && fs.readFileSync('node_modules/.version').toString() === filename) {
    console.log('The node_modules depandence not changed.');
    process.exit();
}

function get(remote, dest, callback) {
    ftp.get(remote, (err, stream) => {
        if (err) throw err;
        stream.on('close', function () {
            ftp.end();
            setTimeout(callback, 1000);
        });
        stream.pipe(fs.createWriteStream(dest));
    });
}

// clean
let deep = 0;
function clean(f_path) {
    if (fs.existsSync(f_path)) {
		try {
			rimraf.sync(f_path);
		}
		catch (e) {
			clean(f_path);
		}
    }
}

process.stdout.write('Clean...');
['node_modules'].forEach(p => clean(p));
clean('node_modules_*');
console.log('Done.');



function install_node_modules(filename) {
    console.log('Done.');
    process.stdout.write('Uncompressing...');
    const command = platform === 'win32' ?
        `build\\install\\rar.exe x -y ${filename} >nul` :
        `tar -xf ${filename}`;
    exec(command);
    clean(filename);
    console.log('Done.');
    fs.writeFileSync('node_modules/.version', filename);
    console.timeEnd('Install node_modules finished.');
}

// get
const ftp = new FtpClient();
const config = { host: envDependencies.node_modules.server, user: 'maview', password: '123456' };

ftp.on('ready', () => {
    console.time('Install node_modules finished.');
    process.stdout.write(`Getting ${remote_path} ...`);

    get(remote_path, filename, () => {
        install_node_modules(filename);
    });
});

ftp.connect(config);
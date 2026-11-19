const fs = require('fs');
const exec = require('child_process').execSync;
const platform = require('os').platform();


// clean
function clean(f_path) {
    if (fs.existsSync(f_path)) {
        if (platform === 'win32') {
            f_path = f_path.replace(/\//g, '\\');
            exec(`del /q /s /f ${f_path} >nul`);
            fs.existsSync(f_path) && exec(`rd /q /s ${f_path} >nul`);
        }
        else {
            exec('rm -rf ' + f_path);
        }
    }
}

process.stdout.write('Clean...');
['node_modules'].forEach(p => clean(p));
console.log('Done.');
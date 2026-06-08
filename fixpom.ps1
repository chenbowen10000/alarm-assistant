$f = 'E:\geek\alarm-assistant\pom.xml'
$c = [System.IO.File]::ReadAllText($f, [System.Text.UTF8Encoding]::new($false))
# Remove duplicate <plugins> wrapper
$c = $c.Replace("        <plugins>`r`n                <plugin>`r`n                    <groupId>org.jacoco</groupId>", "            <plugin>`r`n                    <groupId>org.jacoco</groupId>")
[System.IO.File]::WriteAllText($f, $c, [System.Text.UTF8Encoding]::new($false))
Write-Output 'Fixed'